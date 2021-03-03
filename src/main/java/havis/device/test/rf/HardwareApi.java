package havis.device.test.rf;

import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.KillOperation;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;
import havis.device.test.hardware.RequestCreateTagType;
import havis.device.test.hardware.RequestCreateTagsType;

import java.util.List;

public interface HardwareApi {

	public void connect();

	public void disconnect();

	public List<Short> getConnectedAntennaIDs();

	public RequestCreateTagsType inventory(List<Short> antennas, List<Filter> filters, RssiFilter rssiFilter);

	public TagData getTagData(RequestCreateTagType tag);

	public ReadResult read(ReadOperation rdOp, RequestCreateTagType tag);

	public WriteResult write(WriteOperation wrOp, RequestCreateTagType tag);

	public LockResult lock(LockOperation lOp, RequestCreateTagType tag);

	public KillResult kill(KillOperation kOp, RequestCreateTagType tag);
}