package eu.netide.core.connectivity;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by timvi on 08.07.2015.
 */
public class ShimManager implements IShimManager, IConnectorListener {

    private static final Logger logger = LoggerFactory.getLogger(ShimManager.class);

    private IShimConnector connector;
    private List<IShimMessageListener> shimMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);
    private ExecutorService pool = Executors.newCachedThreadPool();

    public void Start() {
        logger.info("ShimManager started.");
    }

    public void Stop() {
        logger.info("ShimManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message) {
        return connector.SendData(message.toByteRepresentation());
    }

    @Override
    public void OnDataReceived(byte[] data, String originId) {
        Message message;
        try {
            message = NetIPConverter.parseConcreteMessage(data);
        } catch (Exception ex) {
            logger.error("Unable to parse received data from '" + originId + "' (" + Arrays.toString(data) + ").", ex);
            return;
        }
        try {
            listenerLock.acquire();
            for (IShimMessageListener listener : shimMessageListeners) {
                pool.submit(() -> listener.OnShimMessage(message, originId));
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        } finally {
            listenerLock.release();
        }
    }

    public void setConnector(IShimConnector connector) {
        this.connector = connector;
        connector.RegisterShimListener(this);
    }

    public IShimConnector getConnector() {
        return connector;
    }

    public List<IShimMessageListener> getShimMessageListeners() {
        return shimMessageListeners;
    }

    public void setShimMessageListeners(List<IShimMessageListener> shimMessageListeners) throws InterruptedException {
        listenerLock.acquire();
        this.shimMessageListeners = shimMessageListeners == null ? new ArrayList<>() : shimMessageListeners;
        listenerLock.release();
    }
}