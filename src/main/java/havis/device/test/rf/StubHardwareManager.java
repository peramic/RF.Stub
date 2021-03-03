package havis.device.test.rf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.common.Environment;
import havis.device.rf.common.HardwareManager;
import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaConfigurationList;
import havis.device.rf.configuration.AntennaProperties;
import havis.device.rf.configuration.AntennaPropertyList;
import havis.device.rf.configuration.ConnectType;
import havis.device.rf.configuration.RFRegion;
import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.configuration.SingulationControl;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.TagDataList;
import havis.device.rf.tag.operation.KillOperation;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.RequestOperation;
import havis.device.rf.tag.operation.TagOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.hardware.RequestCreateTagType;
import havis.device.test.hardware.RequestCreateTagsType;

public class StubHardwareManager implements HardwareManager {

	private static final Logger logger = Logger
			.getLogger(StubHardwareManager.class.getName());

	private boolean connected;
	private List<Short> connectedAntennas;
	private HardwareApi hwApi;	
	private String regionId;
	private static HardwareMgmt hwMgmt;

	public static void setHardwareMgmt(HardwareMgmt hwMgmt) {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting HardwareMgmt instance to %s.", hwMgmt);
		StubHardwareManager.hwMgmt = hwMgmt;
	}

	public StubHardwareManager() {
		super();

		if (hwMgmt == null)
			throw new NullPointerException(
					"HardwareManagement instance has not been set yet. You may want to call setHardwareMgmt first.");

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "StubHardwareManager instanciated.");
	}

	@Override
	public void openConnection() throws ConnectionException,
			ImplementationException {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Opening hardware connection.");
		this.hwApi = new StubHardwareApi(hwMgmt);
		this.hwApi.connect();
		this.connectedAntennas = new ArrayList<Short>();
		this.connectedAntennas.addAll(this.hwApi.getConnectedAntennaIDs());
		
		if (this.regionId == null)
			this.regionId = Environment.DEFAULT_REGION_ID;
		
		this.connected = true;
	}

	@Override
	public void closeConnection() throws ConnectionException {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Closing hardware connection.");
		this.connectedAntennas = null;
		this.hwApi.disconnect();
		this.hwApi = null;
		this.regionId = null;
		this.connected = false;
	}

	@Override
	public TagDataList execute(List<Short> antennas, List<Filter> filters,
			List<TagOperation> operations, RFConsumer consumer) 
					throws ImplementationException, ParameterException {

		TagDataList result = new TagDataList();
		
		if (logger.isLoggable(Level.FINE))
			logger.log(
					Level.FINE,
					"Execute called with antennas = %s, filters = %s, operations = %s, consumer = %s.",
					new Object[] { antennas, filters, operations, consumer });

		if (!connected)
			throw new ImplementationException(
					"RF hardware not connected. You may want to call openConnection() first.");
		if (regionId == null)
			throw new ImplementationException(
					"No region has been set. You may want to call setRegion() first.");

		if (regionId.equals(Environment.UNSPECIFIED_REGION_ID)) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE,
						"Module region is set to 'Unspecified'. Aborting execution.");
			return result;
		}

		List<Short> effAntennas = getEffectiveAntennaList(antennas);

		if (effAntennas.size() == 0) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE,
						"Empty antenna list received. Aborting execution.");
			return result;
		}

		RequestCreateTagsType tags = hwApi.inventory(effAntennas, filters, rssiFilter);

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Inventory round returned %s tag(s).", tags
					.getTag().size());

		for (RequestCreateTagType tag : tags.getTag()) {
			TagData tagData = hwApi.getTagData(tag);
			result.getEntryList().add(tagData);
			RFCError tagError = null;
			for (TagOperation tagOp : operations)
				tagError = performOperation(tagOp, tagData, tagError, tag, consumer);
		}
		
		return result;
	}

	protected List<Short> getEffectiveAntennaList(List<Short> antennas)
			throws ImplementationException {
		List<Short> antennasOverride = Environment.HARDWARE_MANAGER_ANTENNAS;
		if (antennasOverride != null)
			antennas = antennasOverride;
		if (antennas.size() == 0)
			return antennas;
		if (antennas.get(0) == 0)
			return this.connectedAntennas;
		List<Short> effectiveAntennas = new ArrayList<>();
		for (Short s : antennas)
			if (connectedAntennas.contains(s))
				effectiveAntennas.add(s);
		return effectiveAntennas;
	}
	
	protected RFCError performOperation(TagOperation op, TagData tagData, RFCError tagError, 
			RequestCreateTagType tag, RFConsumer consumer) throws ImplementationException,
			ParameterException {

		if (op instanceof ReadOperation) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Performing read operation %s",
						op.getOperationId());
			ReadResult rRes = null;
			ReadOperation rdOp = (ReadOperation) op;
			if (tagError == null) {
				rRes = hwApi.read(rdOp, tag);
				if (rRes.getResult() != ReadResult.Result.SUCCESS)
					tagError = RFCError.NonSpecificTagError;
			} else {
				rRes = new ReadResult();
				rRes.setReadData(new byte[] {});
				rRes.setOperationId(op.getOperationId());
				rRes.setResult(RFCError.rfcErrorToReadResult(tagError));
			}

			tagData.getResultList().add(rRes);
			if (logger.isLoggable(Level.FINE))
				logger.log(
						Level.FINE,
						"Read result of operation %s was: %s",
						new Object[] { rRes.getOperationId(), rRes.getResult() });
		}

		else if (op instanceof WriteOperation) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Performing write operation %s",
						op.getOperationId());
			WriteResult wRes = null;
			WriteOperation wrOp = (WriteOperation) op;
			if (tagError == null) {
				wRes = hwApi.write(wrOp, tag);

				if (wRes.getResult() != WriteResult.Result.SUCCESS)
					tagError = RFCError.NonSpecificTagError;

			} else {
				wRes = new WriteResult();
				wRes.setOperationId(op.getOperationId());
				wRes.setResult(RFCError.rfcErrorToWriteResult(tagError));
			}

			tagData.getResultList().add(wRes);
			if (logger.isLoggable(Level.FINE))
				logger.log(
						Level.FINE,
						"Write result of operation %s was: %s",
						new Object[] { wRes.getOperationId(), wRes.getResult() });
		}

		else if (op instanceof LockOperation) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Performing lock operation %s",
						op.getOperationId());
			LockResult lRes = null;
			LockOperation lOp = (LockOperation) op;
			if (tagError == null) {
				lRes = hwApi.lock(lOp, tag);
				if (lRes.getResult() != LockResult.Result.SUCCESS)
					tagError = RFCError.NonSpecificTagError;
			} else {
				lRes = new LockResult();
				lRes.setOperationId(op.getOperationId());
				lRes.setResult(RFCError.rfcErrorToLockResult(tagError));
			}

			tagData.getResultList().add(lRes);
			if (logger.isLoggable(Level.FINE))
				logger.log(
						Level.FINE,
						"Lock result of operation %s was: %s",
						new Object[] { lRes.getOperationId(), lRes.getResult() });
		}

		else if (op instanceof KillOperation) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Performing kill operation %s",
						op.getOperationId());
			KillResult kRes = null;
			KillOperation kOp = (KillOperation) op;
			if (tagError == null) {
				kRes = hwApi.kill(kOp, tag);
				if (kRes.getResult() != KillResult.Result.SUCCESS)
					tagError = RFCError.NonSpecificTagError;
			} else {
				kRes = new KillResult();
				kRes.setOperationId(op.getOperationId());
				kRes.setResult(RFCError.rfcErrorToKillResult(tagError));
			}

			tagData.getResultList().add(kRes);
			if (logger.isLoggable(Level.FINE))
				logger.log(
						Level.FINE,
						"Kill result of operation %s was: %s",
						new Object[] { kRes.getOperationId(), kRes.getResult() });
		}

		else if (op instanceof RequestOperation && consumer != null) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "Performing request operation %s",
						op.getOperationId());
			List<TagOperation> additionalOps = consumer.getOperations(tagData);
			if (logger.isLoggable(Level.FINE))
				logger.log(
						Level.FINE,
						"Received additional operations for request operation: %s",
						additionalOps);
			for (TagOperation additionalOp : additionalOps)
				tagError = performOperation(additionalOp, tagData,
						tagError, tag, null);
		}
		return tagError;
	}

	
	@Override
	public String getRegion() {
		return this.regionId;
	}

	@Override
	public void setRegion(RFRegion region,
			AntennaConfigurationList antConfigList) throws ParameterException,
			ImplementationException {

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting region to %s",
					region == null ? "null" : region.getId());

		this.regionId = region.getId();
	}

	
	@Override
	public AntennaPropertyList getAntennaProperties(Map<Short, ConnectType> connectTypeMap)
			throws ImplementationException {
		
		AntennaPropertyList result = new AntennaPropertyList();
		for (short i = 0; i < connectTypeMap.size(); i++) {

			short antennaId = (short) (i + 1);
			ConnectType conType = connectTypeMap.get(antennaId);
			boolean conState = false;

			if (conType == null) conType = ConnectType.AUTO;

			switch (conType) {
				case TRUE:
					conState = true;
					break;
				case FALSE:
					conState = false;
					break;
				default:
					conState = this.hwApi.getConnectedAntennaIDs().contains(antennaId);
					break;
			}
			AntennaProperties ap = new AntennaProperties();
			ap.setId(antennaId);
			ap.setConnected(conState);
			ap.setGain((short) 0);
			result.getEntryList().add(ap);
		}

		return result;
	}

	private void setConnected(short antenna, boolean connected) throws ImplementationException {
		logger.entering(this.getClass().getName(), "setConnected", new Object[] { antenna, connected  });
		if (connected) {
			if (!connectedAntennas.contains(antenna))
				connectedAntennas.add(antenna);													
		} else {
			if (connectedAntennas.contains(antenna))
				connectedAntennas.remove((Short) antenna);
		}
		
		logger.exiting(this.getClass().getName(), "setConnected");
	}
		
	@Override
	public void installFirmware() {
		
	}
	
	
	@Override
	public String getFirmwareVersion() {
		return "1.0";
	}


	@Override
	public void setAntennaConfiguration(AntennaConfiguration antennaConfiguration, RegulatoryCapabilities regulatoryCapabilities, boolean forceTune)
			throws ParameterException, ImplementationException {
		
		if (logger.isLoggable(Level.FINER))
			logger.entering(this.getClass().getName(), "setAntennaConfiguration", new Object[] { 
				RFUtils.serialize(antennaConfiguration), RFUtils.serialize(regulatoryCapabilities) });

		short antenna = antennaConfiguration.getId();		
		ConnectType connect = antennaConfiguration.getConnect();

		if (connect == null)
			connect = ConnectType.AUTO;

		switch (connect) {
			case TRUE:
				setConnected(antenna, true);
				break;
			case FALSE:
				setConnected(antenna, false);
				break;
			default:
				setConnected(antenna, this.hwApi.getConnectedAntennaIDs().contains(antenna));
				break;
		}
		
		logger.exiting(this.getClass().getName(), "setAntennaConfiguration");
	}

	private RssiFilter rssiFilter = new RssiFilter();
	private SingulationControl singCtl = new SingulationControl();
	
	@Override
	public RssiFilter getRssiFilter() {
		return rssiFilter;
	}

	@Override
	public SingulationControl getSingulationControl() {
		return singCtl;
	}

	@Override
	public void setRssiFilter(RssiFilter rssiFilter) throws ImplementationException {
		this.rssiFilter = rssiFilter;		
	}

	@Override
	public void setSingulationControl(SingulationControl singCtl) throws ImplementationException {
		this.singCtl = singCtl;		
	}

	@Override
	public int getMaxAntennas() {		
		/* large number to avoid antenna entries to be removed from config */
		return 99;
	}
	
}
