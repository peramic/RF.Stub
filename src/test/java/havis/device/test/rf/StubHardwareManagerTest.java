package havis.device.test.rf;

import static mockit.Deencapsulation.getField;
import static mockit.Deencapsulation.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.common.Environment;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaConfigurationList;
import havis.device.rf.configuration.AntennaPropertyList;
import havis.device.rf.configuration.ConnectType;
import havis.device.rf.configuration.RFRegion;
import havis.device.rf.configuration.RssiFilter;
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
import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class StubHardwareManagerTest {

	private final static Logger log = Logger.getLogger(StubHardwareManager.class.getName());
	
	@Mocked
	HardwareMgmt hwMgmt;

	@Mocked
	StubHardwareApi hwApi;
	
	@Before
	public void setup() {
		StubHardwareManager.setHardwareMgmt(hwMgmt);
		log.setLevel(Level.ALL);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testOpenConnection(@Mocked final Environment env) throws ConnectionException, ImplementationException {		
		
		new NonStrictExpectations() {{
			hwApi.getConnectedAntennaIDs();
			result = Arrays.asList((short)1,(short)2);
		}};
				
		setField(Environment.class, "DEFAULT_REGION_ID", "EU");		
		StubHardwareManager shm = new StubHardwareManager();

		assertEquals(getField(shm, "connected"), false);
		assertNull(getField(shm, "connectedAntennas"));
		assertNull(getField(shm, "hwApi"));

		shm.openConnection();

		assertEquals(true, getField(shm, "connected"));
		assertEquals("EU", getField(shm, "regionId"));
		assertTrue(getField(shm, "connectedAntennas") != null);
		assertTrue(((List<Short>)getField(shm, "connectedAntennas")).contains((short)1));
		assertTrue(((List<Short>)getField(shm, "connectedAntennas")).contains((short)2));
		assertTrue(getField(shm, "hwApi") != null);		
	}
	
	@Test
	public void testCloseConnection() throws ConnectionException {
		StubHardwareManager shm = new StubHardwareManager();

		setField(shm, "connected", true);
		setField(shm, "connectedAntennas", new ArrayList<>());
		setField(shm, "hwApi", new StubHardwareApi(hwMgmt));

		shm.closeConnection();

		assertEquals(getField(shm, "connected"), false);
		assertNull(getField(shm, "connectedAntennas"));
		assertNull(getField(shm, "hwApi"));
		assertNull(getField(shm, "regionId"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testExecute(
			@Mocked final StubHardwareApi hwApi,
			@Mocked final RequestCreateTagsType tags,
			@Mocked final RequestCreateTagType tag,
			@Mocked final ReadOperation rdOp,
			@Mocked final RFConsumer consumer,
			@Mocked final AntennaConfigurationList acl,
			@Mocked final TagData tagData,
			@Mocked final Environment env)
			throws ImplementationException, ParameterException {

		final StubHardwareManager shm = new StubHardwareManager();

		/* call without being connected */
		try {
			shm.execute(new ArrayList<Short>(), new ArrayList<Filter>(), new ArrayList<TagOperation>(), consumer);
			fail("Exception expected but none thrown.");
		} catch (Exception e) {
			
		}

		/* call with region null */
		try {
			setField(shm, "connected", true);
			setField(shm, "hwApi", hwApi);
			shm.execute(new ArrayList<Short>(), new ArrayList<Filter>(), new ArrayList<TagOperation>(), consumer);
			fail("Exception expected but none thrown.");
		} catch (Exception e) {
			
		}
		
		/* call with region unspecified */
		setField(shm, "regionId", "Unspecified");
		final TagDataList result1 =	shm.execute(new ArrayList<Short>(), new ArrayList<Filter>(), new ArrayList<TagOperation>(), consumer);
		assertEquals(0, result1.getEntryList().size());

		/* call with empty antenna list */
		setField(shm, "regionId", "EU");
		final TagDataList result2 =	shm.execute(new ArrayList<Short>(), new ArrayList<Filter>(), new ArrayList<TagOperation>(), consumer);
		assertEquals(0, result2.getEntryList().size());
		
		/* call with non-empty antenna list */
		new Expectations(shm) {
			{
				hwApi.inventory(withInstanceOf(List.class),withInstanceOf(List.class), withInstanceOf(RssiFilter.class));
				result = tags;

				tags.getTag();
				result = Arrays.asList(new RequestCreateTagType[] { tag });

				hwApi.getTagData(withInstanceOf(RequestCreateTagType.class));
				result = tagData;
				
				shm.performOperation((ReadOperation)any, (TagData)any, (RFCError)any, 
					(RequestCreateTagType)any, (RFConsumer)any);
				result = RFCError.NonSpecificTagError;
			}
		};
		
		setField(Environment.class, "HARDWARE_MANAGER_ANTENNAS",Arrays.asList(new Short[] {(short) 1, (short) 2 }));
		setField(shm, "connectedAntennas", Arrays.asList((short)1, (short)2));
		final TagDataList result3 =	shm.execute(Arrays.asList((short)1), new ArrayList<Filter>(),
			Arrays.asList(new TagOperation[] { rdOp }), consumer);
		assertEquals(1, result3.getEntryList().size());
		
		new Verifications() {{
			shm.performOperation(
				(ReadOperation)any, (TagData)any, (RFCError)any, 
				(RequestCreateTagType)any, (RFConsumer)any);
			times = 1;
		}};
	}

	@Test
	public void testPerformOperation(@Mocked final RequestCreateTagType tag,
			@Mocked final RFConsumer consumer, @Mocked final StubHardwareApi hwApi,
			@Mocked final ReadResult rdRes, @Mocked final WriteResult wrRes,
			@Mocked final LockResult lkRes, @Mocked final KillResult klRes)
			throws ImplementationException, ParameterException {

		StubHardwareManager shm = new StubHardwareManager();
		setField(shm, "hwApi", hwApi);

		/* test read op */
		RFCError tagError = null;
		TagData tagData = new TagData();
		final ReadOperation rdOp = new ReadOperation();
		new NonStrictExpectations() {
			{
				hwApi.read(withInstanceOf(ReadOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = rdRes;

				rdRes.getResult();
				result = ReadResult.Result.SUCCESS;
			}
		};

		tagError = shm.performOperation(rdOp, tagData, tagError, tag,
				consumer);
		assertNull(tagError);

		new Verifications() {
			{
				hwApi.read(withInstanceLike(rdOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {
			{
				hwApi.read(withInstanceOf(ReadOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = rdRes;

				rdRes.getResult();
				result = ReadResult.Result.INCORRECT_PASSWORD_ERROR;
			}
		};

		tagError = shm.performOperation(rdOp, tagData, tagError, tag,
				consumer);
		assertEquals(tagError, RFCError.NonSpecificTagError);

		new Verifications() {
			{
				hwApi.read(withInstanceLike(rdOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {

		};

		tagError = RFCError.NonSpecificTagError;
		tagError = shm.performOperation(rdOp, tagData, tagError, tag,
				consumer);

		new Verifications() {
			{
				hwApi.read(withInstanceLike(rdOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 0;
			}
		};

		/* test write op */
		tagError = null;
		tagData = new TagData();
		final WriteOperation wrOp = new WriteOperation();
		new NonStrictExpectations() {
			{
				hwApi.write(withInstanceOf(WriteOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = wrRes;

				wrRes.getResult();
				result = WriteResult.Result.SUCCESS;
			}
		};

		tagError = shm.performOperation(wrOp, tagData, tagError, tag, consumer);
		assertNull(tagError);

		new Verifications() {
			{
				hwApi.write(withInstanceLike(wrOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {
			{
				hwApi.write(withInstanceOf(WriteOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = wrRes;

				wrRes.getResult();
				result = WriteResult.Result.INCORRECT_PASSWORD_ERROR;
			}
		};

		tagError = shm.performOperation(wrOp, tagData, tagError, tag, consumer);
		assertEquals(tagError, RFCError.NonSpecificTagError);

		new Verifications() {
			{
				hwApi.write(withInstanceLike(wrOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {

		};

		tagError = RFCError.NonSpecificTagError;
		tagError = shm.performOperation(wrOp, tagData, tagError, tag, consumer);

		new Verifications() {
			{
				hwApi.write(withInstanceLike(wrOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 0;
			}
		};

		/* test lock op */
		tagError = null;
		tagData = new TagData();
		final LockOperation lkOp = new LockOperation();
		new NonStrictExpectations() {
			{
				hwApi.lock(withInstanceOf(LockOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = lkRes;

				lkRes.getResult();
				result = LockResult.Result.SUCCESS;
			}
		};

		tagError = shm.performOperation(lkOp, tagData, tagError, tag, consumer);
		assertNull(tagError);

		new Verifications() {
			{
				hwApi.lock(withInstanceLike(lkOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {
			{
				hwApi.lock(withInstanceOf(LockOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = lkRes;

				lkRes.getResult();
				result = LockResult.Result.INCORRECT_PASSWORD_ERROR;
			}
		};

		tagError = shm.performOperation(lkOp, tagData, tagError, tag, consumer);
		assertEquals(tagError, RFCError.NonSpecificTagError);

		new Verifications() {
			{
				hwApi.lock(withInstanceLike(lkOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		tagError = RFCError.NonSpecificTagError;
		tagError = shm.performOperation(lkOp, tagData, tagError, tag, consumer);

		new NonStrictExpectations() {
		};

		new Verifications() {
			{
				hwApi.lock(withInstanceLike(lkOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 0;
			}
		};

		/* test kill op */
		tagError = null;
		tagData = new TagData();
		final KillOperation klOp = new KillOperation();
		new NonStrictExpectations() {
			{
				hwApi.kill(withInstanceOf(KillOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = klRes;

				klRes.getResult();
				result = KillResult.Result.SUCCESS;
			}
		};

		tagError = shm.performOperation(klOp, tagData, tagError, tag, consumer);
		assertNull(tagError);

		new Verifications() {
			{
				hwApi.kill(withInstanceLike(klOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		new NonStrictExpectations() {
			{
				hwApi.kill(withInstanceOf(KillOperation.class),
						withInstanceOf(RequestCreateTagType.class));
				result = klRes;

				klRes.getResult();
				result = KillResult.Result.INCORRECT_PASSWORD_ERROR;
			}
		};

		tagError = shm.performOperation(klOp, tagData, tagError, tag, consumer);
		assertEquals(tagError, RFCError.NonSpecificTagError);

		new Verifications() {
			{
				hwApi.kill(withInstanceLike(klOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

		tagError = RFCError.NonSpecificTagError;
		tagError = shm.performOperation(klOp, tagData, tagError, tag, consumer);

		new NonStrictExpectations() {
		};

		new Verifications() {
			{
				hwApi.kill(withInstanceLike(klOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 0;
			}
		};

		/* test req op */
		tagError = null;
		tagData = new TagData();
		final RequestOperation rqOp = new RequestOperation();

		new NonStrictExpectations() {
			{
				consumer.getOperations(withInstanceOf(TagData.class));
				result = Arrays.asList(new TagOperation[] { rdOp });

				hwApi.read(withInstanceLike(rdOp),
						withInstanceOf(RequestCreateTagType.class));
				result = rdRes;

				rdRes.getResult();
				result = ReadResult.Result.SUCCESS;
			}
		};

		tagError = shm.performOperation(rqOp, tagData, tagError, tag, consumer);

		new Verifications() {
			{
				hwApi.read(withInstanceLike(rdOp),
						withInstanceOf(RequestCreateTagType.class));
				times = 1;
			}
		};

	}

	@Test
	public void testGetAntennaProperties(@Mocked final HardwareApi hwApi) throws ImplementationException {
		final StubHardwareManager shm = new StubHardwareManager();
		setField(shm, "hwApi", hwApi);
		final Map<Short, ConnectType> connectTypeMap = new HashMap<>();
		
		new Expectations() {{
			hwApi.getConnectedAntennaIDs();
			result = Arrays.asList((short)1);
		}};
		
		/*
		 * Test: 
		 * 	- two antennas, both having a connect type of FALSE
		 * Expected:
		 * 	- antenna property list with two antennas both being not connected  
		 */		
		connectTypeMap.put((short)1, ConnectType.FALSE);
		connectTypeMap.put((short)2, ConnectType.FALSE);
		AntennaPropertyList antennaProperties = shm.getAntennaProperties(connectTypeMap);
		
		assertEquals(connectTypeMap.size(), antennaProperties.getEntryList().size());
		assertEquals(false, antennaProperties.getEntryList().get(0).isConnected());
		assertEquals(false, antennaProperties.getEntryList().get(1).isConnected());
		
		/*
		 * Test: 
		 * 	- two antennas, both having a connect type of TRUE
		 * Expected:
		 * 	- antenna property list with two antennas both being connected  
		 */		
		connectTypeMap.put((short)1, ConnectType.TRUE);
		connectTypeMap.put((short)2, ConnectType.TRUE);
		antennaProperties = shm.getAntennaProperties(connectTypeMap);
		
		assertEquals(connectTypeMap.size(), antennaProperties.getEntryList().size());
		assertEquals(true, antennaProperties.getEntryList().get(0).isConnected());
		assertEquals(true, antennaProperties.getEntryList().get(1).isConnected());
		
		/*
		 * Test: 
		 * 	- two antennas, both having a connect type of AUTO
		 * Expected:
		 * 	- antenna property list with two antennas, one being connected 
		 *    and the other being not connected  
		 */
		
		
		connectTypeMap.put((short)1, ConnectType.AUTO);
		connectTypeMap.put((short)2, ConnectType.AUTO);
		antennaProperties = shm.getAntennaProperties(connectTypeMap);
		
		assertEquals(connectTypeMap.size(), antennaProperties.getEntryList().size());
		assertEquals(true, antennaProperties.getEntryList().get(0).isConnected());
		assertEquals(false, antennaProperties.getEntryList().get(1).isConnected());
		
		/*
		 * Test: 
		 * 	- two antennas, both having a connect type of null
		 * Expected:
		 * 	- same behavior as connect type AUTO for both antennas    
		 */		
		connectTypeMap.put((short)1, null);
		connectTypeMap.put((short)2, null);
		antennaProperties = shm.getAntennaProperties(connectTypeMap);
		
		assertEquals(connectTypeMap.size(), antennaProperties.getEntryList().size());
		assertEquals(true, antennaProperties.getEntryList().get(0).isConnected());
		assertEquals(false, antennaProperties.getEntryList().get(1).isConnected());					
	}

	@Test
	public void testGetRegion() {
		StubHardwareManager shm = new StubHardwareManager();
		assertNull(shm.getRegion());
		setField(shm, "regionId", "TEST");
		assertEquals(shm.getRegion(), "TEST");
	}

	@Test
	public void testSetRegion() throws ParameterException,
			ImplementationException {
		StubHardwareManager shm = new StubHardwareManager();
		assertNull(getField(shm, "regionId"));

		RFRegion reg = new RFRegion();
		reg.setId("TEST");
		shm.setRegion(reg, null);

		assertEquals(getField(shm, "regionId"), reg.getId());
	}

	@Test
	public void testGetEffectiveAntennaList(@Mocked final Environment env)
			throws ImplementationException {
		final List<Short> connectedAntennas = Arrays.asList(new Short[] { 1, 2,
				3, 4 });
		final List<Short> antennasOverride = Arrays.asList(new Short[] { 1, 2,
				5, 6 });

		setField(Environment.class, "HARDWARE_MANAGER_ANTENNAS", antennasOverride);
		
		StubHardwareManager shm = new StubHardwareManager();
		setField(shm, "connectedAntennas",
				Arrays.asList(new Short[] { 1, 2, 3, 4 }));

		List<Short> antennas = Arrays.asList(new Short[] { 1, 2, 3, 4 });
		assertEquals(shm.getEffectiveAntennaList(antennas),
				Arrays.asList(new Short[] { 1, 2 }));

		setField(Environment.class, "HARDWARE_MANAGER_ANTENNAS", null);

		antennas = Arrays.asList(new Short[] {});
		assertEquals(shm.getEffectiveAntennaList(antennas).size(), 0);

		antennas = Arrays.asList(new Short[] { 0 });
		assertEquals(shm.getEffectiveAntennaList(antennas), connectedAntennas);
	}

	@Test
	public void testSetAntennaConfiguration(@Mocked final HardwareApi hwApi, @Mocked final RegulatoryCapabilities regulatoryCapabilities) throws ParameterException, ImplementationException {
		
		final StubHardwareManager nurHwMgr = new StubHardwareManager();		
		final List<Short> connectedAntennas = new ArrayList<>();
		setField(nurHwMgr, "connectedAntennas", connectedAntennas);		
		setField(nurHwMgr, "hwApi", hwApi);
		
		new NonStrictExpectations() {{
			hwApi.getConnectedAntennaIDs();
			result = Arrays.asList((short)1);
		}};
		
		final AntennaConfiguration antennaConfiguration1 = new AntennaConfiguration();
		antennaConfiguration1.setId((short)1);
		antennaConfiguration1.setTransmitPower((short)0);
		
		final AntennaConfiguration antennaConfiguration2 = new AntennaConfiguration();
		antennaConfiguration2.setId((short)2);
		antennaConfiguration2.setTransmitPower((short)0);
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 1 
		 *  - connect type is true
		 * 	- force tune param is false 
		 * Expected:
		 * 	- antenna 1 is added to connectedAntennas list		
		 */
		
		antennaConfiguration1.setConnect(ConnectType.TRUE);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration1, regulatoryCapabilities, false);
		
		new Verifications() {{
			assertTrue(connectedAntennas.contains(antennaConfiguration1.getId()));			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 1 
		 *  - connect type is false
		 * 	- force tune param is false 
		 * Expected:
		 * 	- antenna 1 is removed from connectedAntennas list
		 */
		
		antennaConfiguration1.setConnect(ConnectType.FALSE);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration1, regulatoryCapabilities, false);
		
		new Verifications() {{
			assertTrue(!connectedAntennas.contains(antennaConfiguration1.getId()));			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 1 
		 *  - connect type is auto
		 * 	- force tune param is false 
		 * Expected:
		 * 	- antenna 1 is added from connectedAntennas list		
		 */
		
		antennaConfiguration1.setConnect(ConnectType.AUTO);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration1, regulatoryCapabilities, false);
		
		new Verifications() {{
			assertTrue(connectedAntennas.contains(antennaConfiguration1.getId()));			
			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 2 
		 *  - connect type is true
		 * 	- force tune param is false
		 * Expected:
		 * 	- antenna 2 is added to connectedAntennas list		
		 */
		
		antennaConfiguration2.setConnect(ConnectType.TRUE);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration2, regulatoryCapabilities, true);
		
		new Verifications() {{
			assertTrue(connectedAntennas.contains(antennaConfiguration2.getId()));
			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 2 
		 *  - connect type is true
		 * 	- force tune param is true
		 * Expected:
		 * 	- antenna 2 remains in connectedAntennas list 
		 */
		
		antennaConfiguration2.setConnect(ConnectType.TRUE);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration2, regulatoryCapabilities, true);
		
		new Verifications() {{
			assertTrue(connectedAntennas.contains(antennaConfiguration2.getId()));
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 2 
		 *  - connect type is auto
		 * 	- force tune param is false
		 * Expected:
		 * 	- antenna 2 is removed from connectedAntennas list 
		 */
		
		antennaConfiguration2.setConnect(ConnectType.AUTO);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration2, regulatoryCapabilities, false);
		
		new Verifications() {{
			assertTrue(!connectedAntennas.contains(antennaConfiguration2.getId()));			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 2 
		 *  - connect type is true
		 * 	- force tune param is true
		 * Expected:
		 * 	- antenna 2 is added to connectedAntennas list 
		 */
				
		antennaConfiguration2.setConnect(ConnectType.TRUE);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration2, regulatoryCapabilities, true);
		
		new Verifications() {{
			assertTrue(connectedAntennas.contains(antennaConfiguration2.getId()));			
		}};
		
		/* 
		 * Test:
		 * 	- set antenna configuration for antenna 2 
		 *  - connect type is null
		 * 	- force tune param is true
		 * Expected:
		 * 	- antenna 2 is removed from connectedAntennas list (since connect type null is treated as auto) 
		 */
				
		antennaConfiguration2.setConnect(null);
		nurHwMgr.setAntennaConfiguration(antennaConfiguration2, regulatoryCapabilities, true);
		
		new Verifications() {{
			assertTrue(!connectedAntennas.contains(antennaConfiguration2.getId()));
		}};
	}
}
