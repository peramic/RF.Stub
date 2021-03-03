package havis.device.test.rf.osgi;

import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import havis.device.rf.RFDevice;
import havis.device.rf.common.CommunicationHandler;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.rf.StubHardwareManager;

/**
 * <p>
 * OSGi bundle Activator class that registers an RFControllerFactory as service.
 * The service is registered using the naming from bundle properties that have
 * to be provided by the OSGi container. The name(s) the service is registered
 * with, consists of a host and a port and can be retrieved using the pattern
 * <b>(&(host=&lt;HOST&gt;)(port=&lt;PORT&gt;))</b>. The bundle properties to
 * declare the names are <b>havis.embedded.rfc.service.host.<i>i</i></b> and
 * <b>havis.embedded.rfc.service.port.<i>i</i></b> whereas <b><i>i</i></b> is a
 * number ranging from 0 to N, resulting in the service being registered N+1
 * times under the corresponding names
 * </p>
 * 
 * <p>
 * A registered factory's getInstance method is synchronized as well as the
 * hardware access.
 * </p>
 * 
 */

public class Activator implements BundleActivator {

	Logger log = Logger.getLogger(Activator.class.getName());

	private ServiceRegistration<?> serviceReg;
	private ServiceTracker<HardwareMgmt, HardwareMgmt> hwMgmtTracker;

	/**
	 * Is called by the OSGi container once the bundle is started. This listener
	 * method is used to register the service as described above.
	 * 
	 * @param bundleContext
	 *            the BundleContext instance.
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		final BundleContext ctx = bundleContext;

		hwMgmtTracker = new ServiceTracker<HardwareMgmt, HardwareMgmt>(ctx,
				HardwareMgmt.class, null) {
			@Override
			public HardwareMgmt addingService(
					ServiceReference<HardwareMgmt> reference) {
				HardwareMgmt hwMgmt = super.addingService(reference);
				StubHardwareManager.setHardwareMgmt(hwMgmt);
				Activator.this.registerService(ctx);
				return hwMgmt;
			}

			@Override
			public void removedService(
					ServiceReference<HardwareMgmt> reference,
					HardwareMgmt service) {
				Activator.this.unregisterService();
				StubHardwareManager.setHardwareMgmt(null);
				super.removedService(reference, service);
			}
		};

		hwMgmtTracker.open();
	}

	/**
	 * Is called by the OSGi container once the bundle is stopped. This listener
	 * method is called to unregister an service instances.
	 * 
	 * @param bundleContext
	 *            the BundleContext instance.
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		unregisterService();
		hwMgmtTracker.close();
	}

	protected void registerService(BundleContext bundleContext) {
		if (serviceReg != null)
			return;

		this.serviceReg = bundleContext.registerService(
				RFDevice.class.getName(),
				new ServiceFactory<RFDevice>() {

					@Override
					public RFDevice getService(Bundle bundle,
							ServiceRegistration<RFDevice> registration) {
						ClassLoader current = Thread.currentThread().getContextClassLoader();
						try {
							Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
							return new CommunicationHandler();
						} finally {
							Thread.currentThread().setContextClassLoader(current);
						}
					}

					@Override
					public void ungetService(Bundle bundle,
							ServiceRegistration<RFDevice> registration,
							RFDevice service) {
						/* RFU */
					}

				}, null);

	}

	protected void unregisterService() {
		if (this.serviceReg == null)
			return;
		this.serviceReg.unregister();
		this.serviceReg = null;
	}
}
