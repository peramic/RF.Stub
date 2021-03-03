package havis.device.test.rf;

import static org.junit.Assert.*;

import static mockit.Deencapsulation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.KillOperation;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.LockOperation.Field;
import havis.device.rf.tag.operation.LockOperation.Privilege;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;
import havis.device.test.hardware.DataType;
import havis.device.test.hardware.FieldType;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.hardware.LockFieldNameEnumeration;
import havis.device.test.hardware.LockType;
import havis.device.test.hardware.LocksType;
import havis.device.test.hardware.MemoryBankNameEnumeration;
import havis.device.test.hardware.MemoryBankType;
import havis.device.test.hardware.MemoryBanksType;
import havis.device.test.hardware.RequestAbstractType;
import havis.device.test.hardware.RequestCreateTagAntennaType;
import havis.device.test.hardware.RequestCreateTagAntennasType;
import havis.device.test.hardware.RequestCreateTagType;
import havis.device.test.hardware.RequestCreateTagsType;
import havis.device.test.hardware.RequestCreateType;
import havis.device.test.hardware.RequestDeleteType;
import havis.device.test.hardware.RequestReadType;
import havis.device.test.hardware.RequestType;
import havis.device.test.hardware.RequestUpdateType;
import havis.device.test.hardware.ResponseReadAntennaType;
import havis.device.test.hardware.ResponseReadAntennasType;
import havis.device.test.hardware.ResponseReadType;
import havis.device.test.hardware.ResponseType;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class StubHardwareApiTest {

	@Mocked HardwareMgmt manager;
	
	private RequestCreateTagType testTag;
	
	@Before
	public void setup() {
		RequestCreateTagType testTag = new RequestCreateTagType();
		testTag.setTagId(UUID.randomUUID().toString());
		
		testTag.setAntennas(new RequestCreateTagAntennasType());
		RequestCreateTagAntennaType antenna = new RequestCreateTagAntennaType();
		antenna.setAntennaId(1);
		antenna.setPeakRSSI(42);
		testTag.getAntennas().getAntenna().add(antenna);
		
		testTag.setMemoryBanks(new MemoryBanksType());
		
		MemoryBankType rsvBank = new MemoryBankType();
		rsvBank.setName(MemoryBankNameEnumeration.PWD_BANK);
		DataType rsvData = new DataType();
		rsvData.setValue("11223344aabbccdd");
		rsvBank.setData(rsvData);
		testTag.getMemoryBanks().getMemoryBank().add(rsvBank);
		
		MemoryBankType epcBank = new MemoryBankType();
		epcBank.setName(MemoryBankNameEnumeration.EPC_BANK);
		DataType epcData = new DataType();
		epcData.setValue("12343400aaaabbbbccccddddeeeeffff");
		epcBank.setData(epcData);
		testTag.getMemoryBanks().getMemoryBank().add(epcBank);
		
		MemoryBankType tidBank = new MemoryBankType();
		tidBank.setName(MemoryBankNameEnumeration.TID_BANK);
		DataType tidData = new DataType();
		tidData.setValue("e28011052000324ddf740012");
		tidBank.setData(tidData);
		testTag.getMemoryBanks().getMemoryBank().add(tidBank);
		
		MemoryBankType usrBank = new MemoryBankType();
		usrBank.setName(MemoryBankNameEnumeration.USER_BANK);
		DataType usrData = new DataType();
		usrData.setValue("aaaabbbbccccddddeeeeffff");
		usrBank.setData(usrData);
		testTag.getMemoryBanks().getMemoryBank().add(usrBank);
		
		this.testTag = testTag;
	}
	
	@Test
	public void testStubHardwareApi() {
		final StubHardwareApi api = new StubHardwareApi(manager);
		assertEquals(manager, getField(api, "manager"));
	}

	@Test
	public void testConnect(@Mocked final ResponseReadAntennasType responseReadAntennasType, @Mocked final ResponseReadAntennaType antenna1, @Mocked final ResponseReadAntennaType antenna2) {
		final StubHardwareApi api = new StubHardwareApi(manager);		
		final List<ResponseReadAntennaType> antennas = new ArrayList<>();
		
		antennas.add(antenna1);
		antennas.add(antenna2);
		
		new NonStrictExpectations(api) {{
			api.getAntennas();
			result = responseReadAntennasType;
			
			responseReadAntennasType.getAntenna();
			result = antennas;
			
			antenna1.getAntennaId();
			result = 1;
			
			antenna2.getAntennaId();
			result = 2;
		}};
		
		api.connect();		
		HashMap<Short, ResponseReadAntennaType> antennaMap = getField(api, "antennas");
		assertEquals(2, antennaMap.size());
		assertTrue(antennaMap.containsKey((short)1));
		assertTrue(antennaMap.containsKey((short)2));
		assertEquals(antenna1, antennaMap.get((short)1));
		assertEquals(antenna2, antennaMap.get((short)2));
	}

	@Test
	public void testDisconnect() {
		final StubHardwareApi api = new StubHardwareApi(manager);		
		setField(api, "antennas", new HashMap<Short, ResponseReadAntennaType>());
		api.disconnect();
		assertNull(getField(api, "antennas"));
	}

	@Test
	public void testGetConnectedAntennaIDs(@Mocked final ResponseReadAntennasType responseReadAntennasType) {
		final StubHardwareApi api = new StubHardwareApi(manager);
		final List<ResponseReadAntennaType> antennas = new ArrayList<>();
		antennas.add(new ResponseReadAntennaType() {{ setAntennaId(1); }});
		antennas.add(new ResponseReadAntennaType() {{ setAntennaId(2); }});
		antennas.add(new ResponseReadAntennaType() {{ setAntennaId(3); }});
		
		new NonStrictExpectations(api) {{			
			api.getAntennas();
			result = responseReadAntennasType;
			
			responseReadAntennasType.getAntenna();
			result = antennas;
		}};
		
		List<Short> antennaIds = api.getConnectedAntennaIDs();
		assertEquals(3, antennaIds.size());
		assertTrue(antennaIds.contains((short)1));
		assertTrue(antennaIds.contains((short)2));
		assertTrue(antennaIds.contains((short)3));
	}

	@Test
	public void testInventory(@Mocked final HardwareMgmt manager, 
			@Mocked final RequestReadType readRequest, 
			@Mocked final ResponseType response,
			@Mocked final ResponseReadType readResponse,
			@Mocked final RequestCreateTagsType tags, 
			@Mocked final RequestCreateTagType tag1, 
			@Mocked final RequestCreateTagType tag2) {
		
		final StubHardwareApi api = new StubHardwareApi(manager);	
		final List<RequestType> requests = new ArrayList<>();
		final List<ResponseType> responses = new ArrayList<>();
		final List<ResponseReadType> readResponses = new ArrayList<>();
		final List<RequestCreateTagType> tagList = new ArrayList<>();
		final List<Short> antennas = new ArrayList<>();
		final List<Filter> filters = new ArrayList<>();
		final RssiFilter rssiFilter = new RssiFilter();
		
		responses.add(response);
		readResponses.add(readResponse);
		
		tagList.add(tag1);
		tagList.add(tag2);
		
		new NonStrictExpectations(api) {{
			api.createReadRequest();
			result = readRequest;
			
			api.createRequest(anyString, (RequestAbstractType)any);
			result = requests;
			
			manager.process(requests);
			result = responses;
			
			response.getChoice();
			result = readResponses;
			
			readResponse.getTags();
			result = null;
		}};
		
		final RequestCreateTagsType invResult0 = api.inventory(antennas, filters, rssiFilter);		
		assertEquals(0, invResult0.getTag().size());		
		
		new NonStrictExpectations() {{
			readResponse.getTags();
			result = tags;
			
			tags.getTag();
			result = tagList; 
			
			api.applyFilters(tag1, antennas, filters, rssiFilter);
			result = true;
			
			api.applyFilters(tag2, antennas, filters, rssiFilter);
			result = true;
		}};
		
		final RequestCreateTagsType invResult1 = api.inventory(antennas, filters, rssiFilter);		
		
		assertEquals(2, invResult1.getTag().size());
		assertEquals(tag1, invResult1.getTag().get(0));
		assertEquals(tag2, invResult1.getTag().get(1));

		new NonStrictExpectations() {{
			api.applyFilters(tag2, antennas, filters, rssiFilter);
			result = false;
		}};
		
		final RequestCreateTagsType invResult2 = api.inventory(antennas, filters, rssiFilter);	
		assertEquals(1, invResult2.getTag().size());
		assertEquals(tag1, invResult2.getTag().get(0));
		
		new NonStrictExpectations() {{
			api.applyFilters(tag2, antennas, filters, rssiFilter);
			result = true;
			
			tag1.isKilled();
			result = Boolean.TRUE;
			
			tag2.isKilled();
			result = Boolean.FALSE;
		}};
		
		tagList.add(tag2);		
		final RequestCreateTagsType invResult3 = api.inventory(antennas, filters, rssiFilter);		
		assertEquals(1, invResult3.getTag().size());
		assertEquals(tag2, invResult3.getTag().get(0));
	}

	@Test
	public void testApplyFilters() {
		RequestCreateTagAntennaType tagAntenna = new RequestCreateTagAntennaType();
		tagAntenna.setAntennaId(1);
		
		RequestCreateTagType tag = new RequestCreateTagType();
		tag.setAntennas(new RequestCreateTagAntennasType());
		tag.getAntennas().getAntenna().add(tagAntenna);
		tag.setMemoryBanks(new MemoryBanksType());
		
		MemoryBankType epcBank = new MemoryBankType();
		epcBank.setName(MemoryBankNameEnumeration.EPC_BANK);
		epcBank.setData(new DataType());
		epcBank.getData().setValue("00001111222244448888aaaabbbbccccddddeeeeffff");
		
		MemoryBankType tidBank = new MemoryBankType();
		tidBank.setName(MemoryBankNameEnumeration.TID_BANK);
		tidBank.setData(new DataType());
		tidBank.getData().setValue("e28000001111222244448888aaaabbbbccccddddeeeeffff");
		
		tag.getMemoryBanks().getMemoryBank().add(epcBank);
		tag.getMemoryBanks().getMemoryBank().add(tidBank);
		
		final StubHardwareApi api = new StubHardwareApi(manager);
		
		List<Filter> filters = new ArrayList<>();
		List<Short> antennas = new ArrayList<>();
		RssiFilter rssiFilter = new RssiFilter();
		antennas.add((short)2);
		
		assertEquals(false, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		antennas.add((short)1);
		assertEquals(true, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		Filter filter1 = new Filter();
		filter1.setData(RFUtils.hexToBytes("2222555588887777"));
		filter1.setBank(RFUtils.BANK_USR);
		filter1.setMask(RFUtils.hexToBytes("0000000000000000"));
		filter1.setBitOffset((short)32);
		filter1.setBitLength((short)64);
		filter1.setMatch(true);
		
		Filter filter2  = new Filter();
		filter2.setData(RFUtils.hexToBytes("e281000022224444"));
		filter2.setBank(RFUtils.BANK_TID);
		filter2.setMask(RFUtils.hexToBytes("0000000000000000"));
		filter2.setBitOffset((short)0);
		filter2.setBitLength((short)64);
		filter2.setMatch(true);
		
		filters.add(filter1);
		filters.add(filter2);

		assertEquals(false, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		filter1.setBank(RFUtils.BANK_TID);
		
		new NonStrictExpectations(api) {{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			result = true;
		}};
		
		assertEquals(true, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		new Verifications(){{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			times = 2;
		}};
		
		filter1.setMask(RFUtils.hexToBytes("ffff0000ffff0000"));
		filter2.setMask(RFUtils.hexToBytes("ffff0000ffff0000"));
				
		new NonStrictExpectations() {{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			result = true;
		}};		
		
		assertEquals(true, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		new Verifications(){{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			times = 4;
		}};
		
		new NonStrictExpectations() {{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			result = false;
		}};		
		
		assertEquals(false, api.applyFilters(tag, antennas, filters, rssiFilter));
		
		new Verifications(){{
			api.applyFilter(withInstanceOf(Filter.class), anyString);
			times = 1;
		}};
	}

	@Test
	public void testApplyFilter() {
		StubHardwareApi api = new StubHardwareApi(manager);
		
		String data = "aaaabbbbcccc";
		String filterData = "aaaabbbb";
		
		Filter filter = new Filter();
		filter.setMatch(true);
		filter.setData(RFUtils.hexToBytes(filterData));
		filter.setBitLength((short)32);
		
		assertEquals(true, api.applyFilter(filter, data));
		filter.setMatch(false);
		assertEquals(false, api.applyFilter(filter, data));
		
		filter.setMatch(true);
		filter.setBitOffset((short)16);		
		assertEquals(false, api.applyFilter(filter, data));
		
		filter.setMatch(false);
		assertEquals(true, api.applyFilter(filter, data));
	}

	@Test
	public void testGetMemoryBank() {
		StubHardwareApi api = new StubHardwareApi(manager);
		
		MemoryBankType rsvBank = new MemoryBankType();
		MemoryBankType epcBank = new MemoryBankType();
		MemoryBankType tidBank = new MemoryBankType();
		MemoryBankType usrBank = new MemoryBankType();
		
		rsvBank.setName(MemoryBankNameEnumeration.PWD_BANK);
		epcBank.setName(MemoryBankNameEnumeration.EPC_BANK);
		tidBank.setName(MemoryBankNameEnumeration.TID_BANK);
		usrBank.setName(MemoryBankNameEnumeration.USER_BANK);
		
		RequestCreateTagType tag = new RequestCreateTagType();
		tag.setMemoryBanks(new MemoryBanksType());
		tag.getMemoryBanks().getMemoryBank().add(rsvBank);
		tag.getMemoryBanks().getMemoryBank().add(epcBank);
		tag.getMemoryBanks().getMemoryBank().add(tidBank);
		tag.getMemoryBanks().getMemoryBank().add(usrBank);
		
		assertEquals(rsvBank, api.getMemoryBank(tag, RFUtils.BANK_PSW));
		assertEquals(epcBank, api.getMemoryBank(tag, RFUtils.BANK_EPC));
		assertEquals(tidBank, api.getMemoryBank(tag, RFUtils.BANK_TID));
		assertEquals(usrBank, api.getMemoryBank(tag, RFUtils.BANK_USR));
		assertNull(api.getMemoryBank(tag, (short)5));
	}

	@Test
	public void testGetTagData() {
		RequestCreateTagType tag = new RequestCreateTagType();
		tag.setTagId(UUID.randomUUID().toString());
		
		tag.setAntennas(new RequestCreateTagAntennasType());
		RequestCreateTagAntennaType antenna = new RequestCreateTagAntennaType();
		antenna.setAntennaId(1);
		antenna.setPeakRSSI(42);
		tag.getAntennas().getAntenna().add(antenna);
		
		tag.setMemoryBanks(new MemoryBanksType());
		MemoryBankType epcBank = new MemoryBankType();
		epcBank.setName(MemoryBankNameEnumeration.EPC_BANK);
		DataType epcData = new DataType();
		epcData.setValue("12343400aaaabbbbccccddddeeeeffff");

		epcBank.setData(epcData);
		tag.getMemoryBanks().getMemoryBank().add(epcBank);
		
		StubHardwareApi api = new StubHardwareApi(manager);
		TagData td = api.getTagData(tag);			
		assertEquals(0x1234, td.getCrc());
		assertEquals(0x3400, td.getPc());
		assertArrayEquals(RFUtils.hexToBytes("aaaabbbbccccddddeeeeffff"), td.getEpc());
		assertEquals(0,  td.getXpc());
		assertEquals(tag.getAntennas().getAntenna().get(0).getPeakRSSI().intValue(), td.getRssi());
		assertEquals(tag.getAntennas().getAntenna().get(0).getAntennaId().intValue(), td.getAntennaID());
				
		epcData.setValue("12343600aaaabbbbccccddddeeeeffff00112233445566778899aabbccddeeff00112233445566778899aabb");
		td = api.getTagData(tag);		
		assertEquals(0x1234, td.getCrc());
		assertEquals(0x3600, td.getPc());
		assertArrayEquals(RFUtils.hexToBytes("aaaabbbbccccddddeeeeffff00112233445566778899aabbccddeeff00112233445566778899aabb"), td.getEpc());
		assertEquals(RFUtils.bytesToShort(RFUtils.hexToBytes("aabb")), td.getXpc());
		assertEquals(tag.getAntennas().getAntenna().get(0).getPeakRSSI().intValue(), td.getRssi());
		assertEquals(tag.getAntennas().getAntenna().get(0).getAntennaId().intValue(), td.getAntennaID());
	}
	
	@Test
	public void testRead() {
		StubHardwareApi api = new StubHardwareApi(manager);

		ReadOperation readOp = new ReadOperation();
		readOp.setBank(RFUtils.BANK_EPC);
		readOp.setLength((short)6);
		readOp.setOffset((short)2);
		
		ReadResult res = api.read(readOp, this.testTag);
		
		assertEquals(ReadResult.Result.SUCCESS, res.getResult());
		assertEquals("AAAABBBBCCCCDDDDEEEEFFFF", RFUtils.bytesToHex(res.getReadData()));
		
		readOp.setPassword(0x11111111);
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		this.testTag.setLocks(new LocksType());
		LockType lock = new LockType();
		FieldType field = new FieldType();
		field.setName(LockFieldNameEnumeration.KILL_PWD);
		field.setLocked(Boolean.TRUE);
		lock.setField(field);
		this.testTag.getLocks().getLock().add(lock);
		
		readOp.setBank(RFUtils.BANK_PSW);
		readOp.setOffset((short)0);
		readOp.setLength((short)2);
		readOp.setPassword(0x00);
		
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.MEMORY_LOCKED_ERROR, res.getResult());
		
		readOp.setPassword(0xaabbccdd);
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.SUCCESS, res.getResult());
		assertEquals("11223344", RFUtils.bytesToHex(res.getReadData()));
		
		testTag.getMemoryBanks().getMemoryBank().get(0).getData().setValue("1122334400000000");
		readOp.setPassword(0x11111111); //wrong psw
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		readOp.setBank(RFUtils.BANK_USR);
		readOp.setOffset((short)0);
		readOp.setLength((short)0);
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.SUCCESS, res.getResult());
		assertEquals("AAAABBBBCCCCDDDDEEEEFFFF", RFUtils.bytesToHex(res.getReadData()));
		
		readOp.setLength((short)100);
		res = api.read(readOp, this.testTag);
		assertEquals(ReadResult.Result.MEMORY_OVERRUN_ERROR, res.getResult());
		
	}

	@Test
	public void testWrite() {
		StubHardwareApi api = new StubHardwareApi(manager);
		
		WriteOperation writeOp = new WriteOperation();
		writeOp.setBank(RFUtils.BANK_TID);
		writeOp.setOffset((short)0);
		writeOp.setData(RFUtils.hexToBytes("aabbccdd"));
		
		WriteResult res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.MEMORY_LOCKED_ERROR, res.getResult());
		
		/* aaaabbbbccccddddeeeeffff */
		writeOp.setBank(RFUtils.BANK_USR);
		writeOp.setOffset((short)2);
		writeOp.setData(RFUtils.hexToBytes("11112222"));
		
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.SUCCESS, res.getResult());
		assertEquals("aaaabbbb11112222eeeeffff", this.testTag.getMemoryBanks().getMemoryBank().get(3).getData().getValue());
		
		this.testTag.setLocks(new LocksType());
		LockType lock = new LockType();
		FieldType field = new FieldType();
		field.setName(LockFieldNameEnumeration.USER_BANK);
		field.setLocked(Boolean.TRUE);
		lock.setField(field);
		this.testTag.getLocks().getLock().add(lock);
		
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.MEMORY_LOCKED_ERROR, res.getResult());
		
		writeOp.setPassword(0x11111111);
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		testTag.getMemoryBanks().getMemoryBank().get(0).getData().setValue("1122334400000000");
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
	
		this.testTag.getLocks().getLock().clear();
		testTag.getMemoryBanks().getMemoryBank().get(0).getData().setValue("11223344aabbccdd");		
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		writeOp.setOffset((short)32);
		writeOp.setPassword(0x00);
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.MEMORY_OVERRUN_ERROR, res.getResult());
		
		testTag.getMemoryBanks().getMemoryBank().get(3).getData().setValue(null);
		writeOp.setData(RFUtils.hexToBytes("111122223333444455556666777788889999"));
		writeOp.setOffset((short)1);
		res = api.write(writeOp, this.testTag);
		assertEquals(WriteResult.Result.SUCCESS, res.getResult());
		assertEquals("0000111122223333444455556666777788889999", this.testTag.getMemoryBanks().getMemoryBank().get(3).getData().getValue());
				
	}

	@Test
	public void testLock() {
		StubHardwareApi api = new StubHardwareApi(manager);
		LockOperation lockOp = new LockOperation();
		lockOp.setField(Field.USER_MEMORY);
		lockOp.setPrivilege(Privilege.LOCK);
		
	 	LockResult res = api.lock(lockOp, this.testTag);
		assertEquals(LockResult.Result.MEMORY_LOCKED_ERROR, res.getResult());
		
		lockOp.setPassword(0x44332211);
		res = api.lock(lockOp, this.testTag);
		assertEquals(LockResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		testTag.getMemoryBanks().getMemoryBank().get(0).getData().setValue("1122334400000000");
		res = api.lock(lockOp, this.testTag);
		assertEquals(LockResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
				
		testTag.getMemoryBanks().getMemoryBank().get(0).getData().setValue("1122334411111111");
		lockOp.setPassword(0x11111111);

		// no-lock -> permalock => OK
		lockOp.setPrivilege(Privilege.PERMALOCK);
		res = api.lock(lockOp, this.testTag);
		assertEquals(LockResult.Result.SUCCESS, res.getResult());
		
		// permalock -> lock => ERR
		lockOp.setPrivilege(Privilege.LOCK);
		res = api.lock(lockOp, this.testTag);
		assertEquals(LockResult.Result.MEMORY_LOCKED_ERROR, res.getResult());
				
	}

	@Test	
	public void testKill() {
		StubHardwareApi api = new StubHardwareApi(manager);
		KillOperation killOp = new KillOperation();
		
		KillResult res = api.kill(killOp, this.testTag);
		assertEquals(KillResult.Result.ZERO_KILL_PASSWORD_ERROR, res.getResult());
		
		killOp.setKillPassword(0x11111111);
		res = api.kill(killOp, this.testTag);
		assertEquals(KillResult.Result.INCORRECT_PASSWORD_ERROR, res.getResult());
		
		killOp.setKillPassword(0x11223344);
		res = api.kill(killOp, this.testTag);
		assertEquals(KillResult.Result.SUCCESS, res.getResult());
		
		assertEquals(Boolean.TRUE, this.testTag.isKilled());
	}

	@Test
	public void testApplyLockOperation() throws StubHardwareApiException {
		StubHardwareApi api = new StubHardwareApi(manager);
		
		LockOperation lockOp = new LockOperation();
		FieldType field = new FieldType();
		
		/* permalock -> lock => ERROR */		
		try { 
			lockOp.setPrivilege(Privilege.LOCK);
			field.setLocked(Boolean.TRUE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* lock -> permalock => OK */
		lockOp.setPrivilege(Privilege.PERMALOCK);
		field.setLocked(Boolean.TRUE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);
		assertEquals(Boolean.TRUE, field.isLocked());
		assertEquals(Boolean.TRUE, field.isPermanent());
		
		/* permalock -> open => ERROR */
		try { 
			lockOp.setPrivilege(Privilege.UNLOCK);
			field.setLocked(Boolean.TRUE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* permalock -> permaunlock => ERROR */
		try { 
			lockOp.setPrivilege(Privilege.PERMAUNLOCK);
			field.setLocked(Boolean.TRUE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* lock -> unlock => OK */
		lockOp.setPrivilege(Privilege.UNLOCK);
		field.setLocked(Boolean.TRUE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);
		assertEquals(Boolean.FALSE, field.isLocked());
		assertEquals(Boolean.FALSE, field.isPermanent());
		
		/* lock -> permaunlock => OK */
		lockOp.setPrivilege(Privilege.PERMAUNLOCK);
		field.setLocked(Boolean.TRUE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);
		assertEquals(Boolean.FALSE, field.isLocked());
		assertEquals(Boolean.TRUE, field.isPermanent());
		
		/* --- */
		
		/* permaunlock -> lock => ERROR */
		try { 
			lockOp.setPrivilege(Privilege.LOCK);
			field.setLocked(Boolean.FALSE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* permaunlock -> permalock => ERROR */
		try { 
			lockOp.setPrivilege(Privilege.PERMALOCK);
			field.setLocked(Boolean.FALSE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* unlock -> lock => OK */
		lockOp.setPrivilege(Privilege.LOCK);
		field.setLocked(Boolean.FALSE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);		
		assertEquals(Boolean.TRUE, field.isLocked());
		assertEquals(Boolean.FALSE, field.isPermanent());
		
		/* unlock -> permalock => OK */
		lockOp.setPrivilege(Privilege.PERMALOCK);
		field.setLocked(Boolean.FALSE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);		
		assertEquals(Boolean.TRUE, field.isLocked());
		assertEquals(Boolean.TRUE, field.isPermanent());
		
		/* permaunlock -> unlock => ERROR */
		try { 
			lockOp.setPrivilege(Privilege.UNLOCK);
			field.setLocked(Boolean.FALSE);
			field.setPermanent(Boolean.TRUE);
			api.applyLockOperation(field, lockOp);
			fail("Exception expected");
		} 
		catch (StubHardwareApiException ex) { }
		
		/* unlock -> permaunlock => OK */
		lockOp.setPrivilege(Privilege.PERMAUNLOCK);
		field.setLocked(Boolean.FALSE);
		field.setPermanent(Boolean.FALSE);
		api.applyLockOperation(field, lockOp);		
		assertEquals(Boolean.FALSE, field.isLocked());
		assertEquals(Boolean.TRUE, field.isPermanent());
		
	}

	@Test
	public void testIsLocked(@Mocked final FieldType field) {
		final StubHardwareApi api = new StubHardwareApi(manager);
		
		new NonStrictExpectations() {{
			field.isLocked();
			result = null;
		}};
		
		assertEquals(false, api.isLocked(field));
		
		new NonStrictExpectations() {{
			field.isLocked();
			result = Boolean.TRUE;
		}};
		
		assertEquals(true, api.isLocked(field));
		
		
		new NonStrictExpectations() {{
			field.isLocked();
			result = Boolean.FALSE;
		}};
		
		assertEquals(false, api.isLocked(field));
	}

	@Test
	public void testIsPermanent(@Mocked final FieldType field) {
		final StubHardwareApi api = new StubHardwareApi(manager);
		
		new NonStrictExpectations() {{
			field.isPermanent();
			result = null;
		}};
		
		assertEquals(false, api.isPermanent(field));
		
		new NonStrictExpectations() {{
			field.isPermanent();
			result = Boolean.TRUE;
		}};
		
		assertEquals(true, api.isPermanent(field));
		
		
		new NonStrictExpectations() {{
			field.isPermanent();
			result = Boolean.FALSE;
		}};
		
		assertEquals(false, api.isPermanent(field));		
	}

	@Test
	public void testMapLockFieldField() {
		final StubHardwareApi api = new StubHardwareApi(manager);
		assertEquals(LockFieldNameEnumeration.ACCESS_PWD, api.mapLockField(LockOperation.Field.ACCESS_PASSWORD));
		assertEquals(LockFieldNameEnumeration.KILL_PWD, api.mapLockField(LockOperation.Field.KILL_PASSWORD));
		assertEquals(LockFieldNameEnumeration.EPC_BANK, api.mapLockField(LockOperation.Field.EPC_MEMORY));
		assertEquals(LockFieldNameEnumeration.TID_BANK, api.mapLockField(LockOperation.Field.TID_MEMORY));
		assertEquals(LockFieldNameEnumeration.USER_BANK, api.mapLockField(LockOperation.Field.USER_MEMORY));
	}

	@Test
	public void testMapLockFieldShort() {
		final StubHardwareApi api = new StubHardwareApi(manager);
		assertEquals(LockFieldNameEnumeration.EPC_BANK, api.mapLockField(RFUtils.BANK_EPC));
		assertEquals(LockFieldNameEnumeration.TID_BANK, api.mapLockField(RFUtils.BANK_TID));
		assertEquals(LockFieldNameEnumeration.USER_BANK,api.mapLockField(RFUtils.BANK_USR));
		assertNull(api.mapBank((short)99));
	}

	@Test
	public void testMapBank() {
		final StubHardwareApi api = new StubHardwareApi(manager);
		assertEquals(MemoryBankNameEnumeration.PWD_BANK, api.mapBank(RFUtils.BANK_PSW));
		assertEquals(MemoryBankNameEnumeration.EPC_BANK, api.mapBank(RFUtils.BANK_EPC));
		assertEquals(MemoryBankNameEnumeration.TID_BANK, api.mapBank(RFUtils.BANK_TID));
		assertEquals(MemoryBankNameEnumeration.USER_BANK,api.mapBank(RFUtils.BANK_USR));
		assertNull(api.mapBank((short)99));		
	}

	@Test
	public void testInsert() {
		final StubHardwareApi api = new StubHardwareApi(manager);
		String orig = "AAAA";
		
		String new1 = api.insert(orig, "BBBB", 0);
		assertEquals("BBBB", new1);
		
		String new2 = api.insert(orig, "CCCC", 2);
		assertEquals("AACCCC", new2);
		
		String new3 = api.insert(orig, "DDDD", 4);
		assertEquals("AAAADDDD", new3);
		
		String new4 = api.insert(orig, "EEEE", 6);
		assertEquals("AAAA00EEEE", new4);
		
		String new5 = api.insert(orig, "FFFF", -2);
		assertEquals("FFFFAA", new5);
		
		String new6 = api.insert(null, "AAAA", 4);
		assertEquals("0000AAAA", new6);
		
		String new7 = api.insert("", "AAAA", 4);
		assertEquals("0000AAAA", new7);
	}

	@Test
	public void testGetAntennas(final @Mocked ResponseType responseType, final @Mocked ResponseReadType responseReadType, final @Mocked ResponseReadAntennasType responseReadAntennasType) {
		final StubHardwareApi api = new StubHardwareApi(manager);
		final List<RequestType> requests = new ArrayList<>();
		final List<ResponseType> responses = new ArrayList<>();
		final List<ResponseReadType> responseReadTypes = new ArrayList<>();
		
		responses.add(responseType);		
		responseReadTypes.add(responseReadType);
		
		new NonStrictExpectations(api) {{			
			api.createRequest(anyString, (RequestAbstractType)any);
			result = requests;
			
			manager.process(requests);
			result = responses;
			
			responseType.getChoice();
			result = responseReadTypes;
			
			responseReadType.getAntennas();
			result = responseReadAntennasType;
		}};
		
		ResponseReadAntennasType antennas = api.getAntennas();
		assertEquals(responseReadAntennasType, antennas);
		
		new NonStrictExpectations() {{
			responseReadType.getAntennas();
			result = null;
		}};
		
		antennas = api.getAntennas();
		assertNotNull(antennas);
		assertNotEquals(responseReadAntennasType, antennas);
	}
	
	@Test
	public void testCreateRequest(@Mocked final RequestAbstractType requestAbstractType) {
		StubHardwareApi api = new StubHardwareApi(manager);
		final String configId = "configId";
		final List<RequestType> requestTypes = api.createRequest(configId, requestAbstractType);
		
		assertEquals(1, requestTypes.size());
		assertEquals(configId, requestTypes.get(0).getConfigId());
		assertEquals(1, requestTypes.get(0).getChoice().size());
		assertEquals(requestAbstractType, requestTypes.get(0).getChoice().get(0));
	}
	
	@Test
	public void testCreateCreateRequest() {
		StubHardwareApi api = new StubHardwareApi(manager);
		RequestCreateType requestCreateType = api.createCreateRequest();
		assertEquals("0", requestCreateType.getOperationId());
		
		requestCreateType = api.createCreateRequest();
		assertEquals("1", requestCreateType.getOperationId());
	}

	@Test
	public void testCreateUpdateRequest() {
		StubHardwareApi api = new StubHardwareApi(manager);
		RequestUpdateType requestUpdateType = api.createUpdateRequest();
		assertEquals("0", requestUpdateType.getOperationId());
		
		requestUpdateType = api.createUpdateRequest();
		assertEquals("1", requestUpdateType.getOperationId());
	}

	@Test
	public void testCreateDeleteRequest() {
		StubHardwareApi api = new StubHardwareApi(manager);
		RequestDeleteType requestDeleteType = api.createDeleteRequest();
		assertEquals("0", requestDeleteType.getOperationId());
		
		requestDeleteType = api.createDeleteRequest();
		assertEquals("1", requestDeleteType.getOperationId());
	}

	@Test
	public void testCreateReadRequest() {
		StubHardwareApi api = new StubHardwareApi(manager);
		RequestReadType requestReadType = api.createReadRequest();
		assertEquals("0", requestReadType.getOperationId());
		
		requestReadType = api.createReadRequest();
		assertEquals("1", requestReadType.getOperationId());
	}

}
