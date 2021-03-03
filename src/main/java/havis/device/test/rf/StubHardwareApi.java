package havis.device.test.rf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import havis.device.rf.common.util.RFUtils;
import havis.device.rf.configuration.RssiFilter;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.KillOperation;
import havis.device.rf.tag.operation.LockOperation;
import havis.device.rf.tag.operation.LockOperation.Privilege;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.KillResult.Result;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;
import havis.device.test.hardware.DataType;
import havis.device.test.hardware.FieldFormatEnumeration;
import havis.device.test.hardware.FieldType;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.hardware.LockFieldNameEnumeration;
import havis.device.test.hardware.LockType;
import havis.device.test.hardware.LocksType;
import havis.device.test.hardware.MemoryBankNameEnumeration;
import havis.device.test.hardware.MemoryBankType;
import havis.device.test.hardware.RequestAbstractType;
import havis.device.test.hardware.RequestCreateTagAntennaType;
import havis.device.test.hardware.RequestCreateTagType;
import havis.device.test.hardware.RequestCreateTagsType;
import havis.device.test.hardware.RequestCreateType;
import havis.device.test.hardware.RequestDeleteAntennasType;
import havis.device.test.hardware.RequestDeleteTagsType;
import havis.device.test.hardware.RequestDeleteType;
import havis.device.test.hardware.RequestReadType;
import havis.device.test.hardware.RequestType;
import havis.device.test.hardware.RequestUpdateType;
import havis.device.test.hardware.ResponseReadAntennaType;
import havis.device.test.hardware.ResponseReadAntennasType;
import havis.device.test.hardware.ResponseReadType;
import havis.device.test.hardware.ResponseType;

public class StubHardwareApi implements HardwareApi {

	private long operationId = 0;
	private static final String ID = "default";
	private HardwareMgmt manager;
	private Map<Short, ResponseReadAntennaType> antennas;

	private static final long INVENTORY_DELAY_MILLIS = 50;

	public StubHardwareApi(HardwareMgmt manager) {
		super();
		this.manager = manager;
	}

	public void connect() {
		manager.process(createRequest(ID, createCreateRequest()));

		this.antennas = new HashMap<Short, ResponseReadAntennaType>();
		for (ResponseReadAntennaType antenna : getAntennas().getAntenna())
			antennas.put((short) antenna.getAntennaId(), antenna);

	}

	@Override
	public void disconnect() {
		this.antennas = null;
	}

	@Override
	public List<Short> getConnectedAntennaIDs() {
		List<Short> resultList = new ArrayList<>();

		for (ResponseReadAntennaType antenna : getAntennas().getAntenna())
			resultList.add((short) antenna.getAntennaId());

		return resultList;
	}

	@Override
	public RequestCreateTagsType inventory(List<Short> antennas, List<Filter> filters, RssiFilter rssiFilter) {
		RequestReadType read = createReadRequest();
		read.setTags(new RequestDeleteTagsType());
		List<ResponseType> resp = manager.process(createRequest(ID, read));

		RequestCreateTagsType tags = ((ResponseReadType) resp.get(0)
				.getChoice().get(0)).getTags();
		if (tags == null) {
			return new RequestCreateTagsType();
		}
		Iterator<RequestCreateTagType> iTags = tags.getTag().iterator();
		while (iTags.hasNext()) {
			RequestCreateTagType tag = iTags.next();
			if (tag.isKilled() != null && tag.isKilled()
					|| !applyFilters(tag, antennas, filters, rssiFilter))
				

				iTags.remove();
		}

		try {
			Thread.sleep(INVENTORY_DELAY_MILLIS);
		} catch (InterruptedException e) {
			// just stop sleeping
		}

		return tags;
	}

	protected boolean applyFilters(RequestCreateTagType tag,
			List<Short> antennas, List<Filter> filters, RssiFilter rssiFilter) {

		Map<Integer, Integer> tagsAntennaMap = new HashMap<>();
		for (RequestCreateTagAntennaType a : tag.getAntennas().getAntenna())
			tagsAntennaMap.put(a.getAntennaId(), a.getPeakRSSI());

		boolean match = false;
		for (short antenna : antennas)
			if (tagsAntennaMap.containsKey((int) antenna)) {
				if (rssiFilter.getMinRssi() == 0 && rssiFilter.getMaxRssi() == 0) {
					match = true;
					break;
				} else {
					if (rssiFilter.getMinRssi() != 0 && tagsAntennaMap.get((int)antenna) >= rssiFilter.getMinRssi()) {
						match = true;
						break;						
					}
					
					if (rssiFilter.getMaxRssi() != 0 && tagsAntennaMap.get((int)antenna) <= rssiFilter.getMaxRssi()) {
						match = true;
						break;						
					}
				}
			}

		if (!match)
			return false;

		for (Filter filter : filters) {

			MemoryBankType memBank = getMemoryBank(tag, filter.getBank());

			// if memory bank to apply filter to does not exist
			if (memBank == null || memBank.getData() == null) {

				// if filter should match (inclusive) return false, because tag
				// cannot match
				if (filter.isMatch())
					return false;

				// otherwise continue with next filter
				else
					continue;
			}

			// devide filter into subfilters based on mask (result is null if
			// mask is trivial, i.e. 1-only or 0-only)
			List<Filter> subFilters = RFUtils.applyMask(filter);
			if (subFilters == null)
				subFilters = Arrays.asList(new Filter[] { filter });

			// iterate through subfilters and apply each one
			for (Filter subFilter : subFilters)
				// if filter's criterion matches, skip the tag by returning
				// false
				if (!applyFilter(subFilter, memBank.getData().getValue()))
					return false;
		}

		// if all filters have been applied and this point is reached, return
		// true
		// to signal that the tag is not skipped.
		return true;
	}

	protected boolean applyFilter(Filter filter, String dataStr) {
		byte[] data = RFUtils.hexToBytes(dataStr);

		/* create bit set from data */
		BitSet dataBits = RFUtils.bytesToBitSet(data);

		/* create bit set from filter */
		BitSet filterBits = RFUtils.bytesToBitSet(filter.getData());

		/*
		 * Iterate through filter bits and compare to data bit at same address
		 * (+offset).
		 */
		/*
		 * As soon as the bits do not match, return false if the filter is to
		 * match or true if the filter is not to match, thus !filter.isMatch
		 */
		for (int i = 0; i < filter.getBitLength(); i++)
			if (filterBits.get(i) != dataBits.get(i + filter.getBitOffset()))
				return !filter.isMatch();

		/*
		 * If this point is reached, the filter bits matched all data bits. So
		 * return true if filter is to match and false if filter is not to
		 * match, thus filter.isMatch()
		 */

		return filter.isMatch();
	}

	protected MemoryBankType getMemoryBank(RequestCreateTagType tag, short index) {
		MemoryBankNameEnumeration bankName = mapBank(index);
		try {
			for (MemoryBankType bank : tag.getMemoryBanks().getMemoryBank())
				if (bank.getName() == bankName)
					return bank;
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@SuppressWarnings("unused")
	@Override
	public TagData getTagData(RequestCreateTagType tag) {
		TagData td = new TagData();

		String tagId = tag.getTagId();

		int antennaId = tag.getAntennas().getAntenna().get(0).getAntennaId();
		int rssi = tag.getAntennas().getAntenna().get(0).getPeakRSSI();

		MemoryBankType epcBank = null;
		for (MemoryBankType mb : tag.getMemoryBanks().getMemoryBank())
			if (mb.getName() == MemoryBankNameEnumeration.EPC_BANK) {
				epcBank = mb;
				break;
			}

		String epcBankStr = epcBank.getData().getValue();
		byte[] epcBankData = RFUtils.hexToBytes(epcBankStr);

		short crc = RFUtils.bytesToShort(new byte[] { epcBankData[0],
				epcBankData[1] });
		short pc = RFUtils.bytesToShort(new byte[] { epcBankData[2],
				epcBankData[3] });
		byte[] epc = Arrays.copyOfRange(epcBankData, 4, epcBankData.length);

		short xi = (short) ((pc & 0x200) >> 9); // value of XI bit (16h);
		short umi = (short) ((pc & 0x400) >> 10); // value of UMI bit (15h)
		short xpc = 0;

		if (xi != 0 && epcBankData.length >= 44) // get 22nd word of EPC bank
													// (0x210..0x21f)
			xpc = RFUtils.bytesToShort(new byte[] { epcBankData[42],
					epcBankData[43] });

		td.setCrc(crc);
		td.setPc(pc);
		td.setEpc(epc);
		td.setXpc(xpc);
		td.setRssi(rssi);
		td.setAntennaID((short) antennaId);

		return td;
	}

	@Override
	public ReadResult read(ReadOperation rdOp, RequestCreateTagType tag) {
		ReadResult rdRes = new ReadResult();
		rdRes.setOperationId(rdOp.getOperationId());

		FieldType lockedField = getLock(tag, rdOp);
		int accessPsw = 0;
		String dataStr = null;

		/*
		 * Read tag's data from the bank specified in rdOp and the access
		 * password
		 */
		for (MemoryBankType memBank : tag.getMemoryBanks().getMemoryBank()) {
			if (memBank.getName().equals(mapBank(rdOp.getBank())))
				dataStr = memBank.getData().getValue();
			if (memBank.getName().equals(MemoryBankNameEnumeration.PWD_BANK)) {
				try {
					String pswStr = memBank.getData().getValue().substring(8);
					accessPsw = RFUtils
							.bytesToInt(RFUtils.hexToBytes(pswStr));
				} catch (Exception e) {
				}
			}
		}

		/* if a wrong (non-empty) password has been provided result will be INCORRECT_PASSWORD_ERROR */
		if (rdOp.getPassword() != 0 && accessPsw != 0 && rdOp.getPassword() != accessPsw) {
			rdRes.setResult(ReadResult.Result.INCORRECT_PASSWORD_ERROR);
			return rdRes;
		}	
		
		/* locks do only apply when reading the password bank. Other banks can be read despite the lock */
		if (rdOp.getBank() == RFUtils.BANK_PSW) {
		
			/* If bank to read is locked */
			if (lockedField != null && lockedField.isLocked()) {
				/*
				 * If given password is not equal to access psw, set read result
				 * to INCORRECT_PASSWORD_ERROR, as long as password has been provided. 
				 * Set result to MEMORY_LOCKED_ERROR if no password has been provided
				 */
				if (accessPsw != rdOp.getPassword()) {
					if (rdOp.getPassword() == 0)
						rdRes.setResult(ReadResult.Result.MEMORY_LOCKED_ERROR);
					else
						rdRes.setResult(ReadResult.Result.INCORRECT_PASSWORD_ERROR);
					return rdRes;
				}				
			}
		}
		
		try {
			String dataSubStr = null;
			if (rdOp.getLength() > 0)
				dataSubStr = dataStr.substring(rdOp.getOffset() * 4,
						(rdOp.getOffset() + rdOp.getLength()) * 4);
			else
				dataSubStr = dataStr.substring(rdOp.getOffset() * 4);

			rdRes.setReadData(RFUtils.hexToBytes(dataSubStr));
			rdRes.setResult(ReadResult.Result.SUCCESS);
		} catch (IndexOutOfBoundsException e) {
			rdRes.setResult(ReadResult.Result.MEMORY_OVERRUN_ERROR);
		}

		return rdRes;
	}

	private FieldType getLock(RequestCreateTagType tag, ReadOperation rdOp) {
		LocksType locks = tag.getLocks();
		if (locks != null) // If the tag has locked fields

			/*
			 * Iterate through the locks and try to find the one lock for the
			 * field desired.
			 */
			for (LockType lock : locks.getLock()) {

				/*
				 * If the lock matches either USR, TID or EPC bank (PSW bank is
				 * an exceptional case treated below)
				 */
				if (lock.getField().getName()
						.equals(mapLockField(rdOp.getBank())))
					return lock.getField();

				/* If read op reads password bank */
				if (rdOp.getBank() == RFUtils.BANK_PSW) {
					/*
					 * If lock applies to ACCESS_PWD and read op reads access
					 * password
					 */
					if (lock.getField().getName()
							.equals(LockFieldNameEnumeration.ACCESS_PWD)
							&& rdOp.getOffset() + rdOp.getLength() > 2)
						return lock.getField();
					/*
					 * If lock applies to KILL_PWD and read op reads kill
					 * password
					 */
					if (lock.getField().getName()
							.equals(LockFieldNameEnumeration.KILL_PWD)
							&& rdOp.getOffset() + rdOp.getLength() <= 2)
						return lock.getField();
				}
			}
		/* If no such lock exists, return null. */
		return null;
	}

	private FieldType getLock(RequestCreateTagType tag, WriteOperation wrOp) {
		LocksType locks = tag.getLocks();
		if (locks != null) // If the tag has locked fields

			/*
			 * Iterate through the locks and try to find the one lock for the
			 * field desired.
			 */
			for (LockType lock : locks.getLock()) {

				/*
				 * If the lock matches either USR, TID or EPC bank (PSW bank is
				 * an exceptional case treated below)
				 */
				if (lock.getField().getName()
						.equals(mapLockField(wrOp.getBank())))
					return lock.getField();

				/* If read op reads password bank */
				if (wrOp.getBank() == RFUtils.BANK_PSW) {
					/*
					 * If lock applies to ACCESS_PWD and read op reads access
					 * password
					 */
					if (lock.getField().getName()
							.equals(LockFieldNameEnumeration.ACCESS_PWD)
							&& wrOp.getOffset() + wrOp.getData().length > 2)
						return lock.getField();
					/*
					 * If lock applies to KILL_PWD and read op reads kill
					 * password
					 */
					if (lock.getField().getName()
							.equals(LockFieldNameEnumeration.KILL_PWD)
							&& wrOp.getOffset() + wrOp.getData().length <= 2)
						return lock.getField();
				}
			}
		/* If no such lock exists, return null. */
		return null;
	}

	@Override
	public WriteResult write(WriteOperation wrOp, RequestCreateTagType tag) {
		WriteResult wrRes = new WriteResult();
		wrRes.setOperationId(wrOp.getOperationId());

		/* invalid attempt to write to TID bank*/
		if (wrOp.getBank() == RFUtils.BANK_TID) {
			wrRes.setResult(WriteResult.Result.MEMORY_LOCKED_ERROR);
			return wrRes;
		}
		
		FieldType lockedField = getLock(tag, wrOp);
		int accessPsw = 0;
		MemoryBankType bank = null;

		for (MemoryBankType memBank : tag.getMemoryBanks().getMemoryBank()) {
			if (memBank.getName().equals(mapBank(wrOp.getBank())))
				bank = memBank;
			if (memBank.getName().equals(MemoryBankNameEnumeration.PWD_BANK)) {
				try {
					String pswStr = memBank.getData().getValue().substring(8);
					accessPsw = RFUtils.bytesToInt(RFUtils.hexToBytes(pswStr));
				} catch (Exception e) { }
			}
		}

		/* if bank is locked: passwords MUST match */
		if (lockedField != null && lockedField.isLocked()) {

			if (accessPsw != 0) {/* accessPsw has been set*/
				
				/* no password given: MEMORY_LOCKED_ERROR */
				if (wrOp.getPassword() == 0) {
					wrRes.setResult(WriteResult.Result.MEMORY_LOCKED_ERROR);
					return wrRes;
				}
			
				/* access psw and given psw do not match: INCORRECT_PASSWORD_ERROR */
				if (accessPsw != wrOp.getPassword()) {
					wrRes.setResult(WriteResult.Result.INCORRECT_PASSWORD_ERROR);
					return wrRes;
				}				
			} 
			
			else /* acessPsw == 0 */ 
				if (wrOp.getPassword() != 0) { /* given psw is != 0: INCORRECT_PASSWORD_ERROR */
					wrRes.setResult(WriteResult.Result.INCORRECT_PASSWORD_ERROR);
					return wrRes;				
				}
		}
		
		else { /* if bank is not locked: given psw can be 0 or must match */			
			
			/* if given password is != 0: it must match */
			if (wrOp.getPassword() != 0) {				
				/* given psw does not match: INCORRECT_PASSWORD_ERROR */
				if (wrOp.getPassword() != accessPsw) {
					wrRes.setResult(WriteResult.Result.INCORRECT_PASSWORD_ERROR);
					return wrRes;
				} /* else passwords match, which is OK*/
			} /* else no password has been given, which is OK */
			
		}
		
		/* memory bank is empty */
		if (bank.getData() == null || bank.getData().getValue() == null) {
			bank.setData(new DataType());
			bank.getData().setFormat(FieldFormatEnumeration.HEX);
			String newData = insert("", RFUtils.bytesToHex(wrOp.getData()),
					wrOp.getOffset() * 4);
			
			/* write data to memrory bank */
			bank.getData().setValue(newData);

			wrRes.setWordsWritten((short) (wrOp.getData().length / 2));
			wrRes.setResult(WriteResult.Result.SUCCESS);
		}

		else { /*
			    * memory bank already contains data into which the new data
				* will be inserted
				*/
			String memBankData = bank.getData().getValue() != null ? bank
					.getData().getValue() : "";
			
			/* if complete length of data to be written exceeds the initial length of the memory bank, 
			 * result is MEMORY_OVERRUN_ERROR */
			if (wrOp.getData().length * 2 + wrOp.getOffset() * 4 > bank.getData().getValue().length()) 
				wrRes.setResult(WriteResult.Result.MEMORY_OVERRUN_ERROR);
			
			else {
				
				String newData = insert(memBankData,
						RFUtils.bytesToHex(wrOp.getData()), wrOp.getOffset() * 4);
				
				/* write data to memrory bank */
				bank.getData().setValue(newData);
	
				wrRes.setWordsWritten((short) (wrOp.getData().length / 2));
				wrRes.setResult(WriteResult.Result.SUCCESS);
			}
		}

		commit(tag);

		return wrRes;
	}

	@Override
	public LockResult lock(LockOperation lOp, RequestCreateTagType tag) {

		LockResult lRes = new LockResult();
		lRes.setOperationId(lOp.getOperationId());

		int lockPasswd = 0;

		try {
			for (MemoryBankType memBank : tag.getMemoryBanks().getMemoryBank()) {
				if (memBank.getName()
						.equals(MemoryBankNameEnumeration.PWD_BANK)) {
					byte[] lockPasswdBytes = Arrays.copyOfRange(
							RFUtils.hexToBytes(memBank.getData().getValue()),
							4, 8);
					lockPasswd = RFUtils.bytesToInt(lockPasswdBytes);
				}
			}
		} catch (IllegalArgumentException e) {
		}

		if (lockPasswd != 0) {/* lockPsw has been set*/
			
			/* no password given: MEMORY_LOCKED_ERROR */
			if (lOp.getPassword() == 0) {
				lRes.setResult(LockResult.Result.MEMORY_LOCKED_ERROR);
				return lRes;
			}
		
			/* lock psw and given psw do not match: INCORRECT_PASSWORD_ERROR */
			if (lockPasswd != lOp.getPassword()) {
				lRes.setResult(LockResult.Result.INCORRECT_PASSWORD_ERROR);
				return lRes;
			}				
		} 
		else /* lockPasswd == 0 */
			if (lOp.getPassword() != lockPasswd) {
			lRes.setResult(LockResult.Result.INCORRECT_PASSWORD_ERROR);
			return lRes;
		}

		LocksType locks = tag.getLocks();
		if (locks == null) {
			locks = new LocksType();
			tag.setLocks(locks);
		}

		LockType existingLock = null;

		/*
		 * iterate through tag's existing locks and try to find lock matching
		 * field's name
		 */
		for (LockType lock : locks.getLock()) {
			/* if lock object for specific field exists => apply lock */
			if (lock.getField().getName().equals(mapLockField(lOp.getField()))) {
				existingLock = lock;
				try {
					applyLockOperation(lock.getField(), lOp);
				} catch (StubHardwareApiException e) {
					lRes.setResult(LockResult.Result.MEMORY_LOCKED_ERROR);
					return lRes;
				}
			}
		}

		/* if no suitable lock has been found, create a new one */
		if (existingLock == null) {
			LockType newLock = new LockType();
			newLock.setField(new FieldType());
			newLock.getField().setLocked(
					lOp.getPrivilege() == Privilege.LOCK
							|| lOp.getPrivilege() == Privilege.PERMALOCK);
			newLock.getField().setPermanent(
					lOp.getPrivilege() == Privilege.PERMALOCK
							|| lOp.getPrivilege() == Privilege.PERMAUNLOCK);
			newLock.getField().setName(mapLockField(lOp.getField()));
			locks.getLock().add(newLock);
		}

		lRes.setResult(LockResult.Result.SUCCESS);

		commit(tag);

		return lRes;
	}

	@Override
	public KillResult kill(KillOperation kOp, RequestCreateTagType tag) {

		KillResult kRes = new KillResult();
		kRes.setOperationId(kOp.getOperationId());

		if (kOp.getKillPassword() == 0) {
			kRes.setResult(Result.ZERO_KILL_PASSWORD_ERROR);
			return kRes;
		}

		int killPasswd = 0;

		try {
			for (MemoryBankType memBank : tag.getMemoryBanks().getMemoryBank()) {
				if (memBank.getName()
						.equals(MemoryBankNameEnumeration.PWD_BANK)) {
					byte[] killPasswdBytes = Arrays.copyOfRange(
							RFUtils.hexToBytes(memBank.getData().getValue()),
							0, 4);
					killPasswd = RFUtils.bytesToInt(killPasswdBytes);
				}
			}
		} catch (IllegalArgumentException e) {
		}

		if (killPasswd != kOp.getKillPassword()) {
			kRes.setResult(Result.INCORRECT_PASSWORD_ERROR);
			return kRes;
		}

		tag.setKilled(true);

		kRes.setResult(KillResult.Result.SUCCESS);

		commit(tag);

		return kRes;
	}

	protected void applyLockOperation(FieldType existingLock, LockOperation lOp)
			throws StubHardwareApiException {

		if (isLocked(existingLock)) {/* field is locked */

			/* attempt to set lock to perma-locked field => exception */
			if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.LOCK)
				throw new StubHardwareApiException(
						String.format(
								"Failed to lock field %s. Field is already perma-locked.",
								existingLock.getName()));

			/*
			 * attempt to set perma-lock to locked field => change lock to
			 * permalock
			 */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMALOCK) {
				existingLock.setPermanent(true);
			}

			/* attempt to set lock to a locked field => nothing to do */

			/* attempt to set perma-lock to perma-locked field => nothing to do */

			/* attempt to unlock perma-locked field => exception */
			else if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.UNLOCK)
				throw new StubHardwareApiException(String.format(
						"Failed to unlock field %s. Field is perma-locked.",
						existingLock.getName()));

			/* attempt to set perma-unlock to perma-locked field => exception */
			else if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMAUNLOCK) {
				throw new StubHardwareApiException(
						String.format(
								"Failed to perma-unlock field %s. Field is perma-locked.",
								existingLock.getName()));
			}

			/* attempt to unlock to a locked field => remove lock */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.UNLOCK) {
				existingLock.setLocked(false);
			}
			/* attempt to perma-unlock a locked field => permanently remove lock */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMAUNLOCK) {
				existingLock.setLocked(false);
				existingLock.setPermanent(true);
			}
		}

		else { /* field is unlocked */

			/* attempt to set lock to perma-unlocked field => exception */
			if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.LOCK)
				throw new StubHardwareApiException(String.format(
						"Failed to lock field %s. Field is perma-unlocked.",
						existingLock.getName()));

			/* attempt to set perma-lock to perma-unlocked field => exception */
			else if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMALOCK) {
				throw new StubHardwareApiException(
						String.format(
								"Failed to perma-lock field %s. Field is perma-unlocked.",
								existingLock.getName()));
			}

			/* attempt to set lock to an unlocked field => set lock */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.LOCK) {
				existingLock.setLocked(true);
			}

			/* attempt to set perma-lock to an unlocked field => set perma-lock */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMALOCK) {
				existingLock.setLocked(true);
				existingLock.setPermanent(true);
			}

			/* attempt to unlock perma-unlocked field => exception */
			else if (isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.UNLOCK)
				throw new StubHardwareApiException(
						String.format(
								"Failed to unlock field %s. Field is already perma-unlocked.",
								existingLock.getName()));

			/* attempt to perma-unlock to an unlocked field => set perma-unlock */
			else if (!isPermanent(existingLock)
					&& lOp.getPrivilege() == Privilege.PERMAUNLOCK) {
				existingLock.setLocked(false);
				existingLock.setPermanent(true);
			}

			/* attempt to perma-unlock to perma-unlocked field => do nothing */

			/* attempt to unlock an unlocked field => do nothing */
		}
	}

	protected boolean isLocked(FieldType field) {
		return field.isLocked() != null && field.isLocked().booleanValue();
	}

	protected boolean isPermanent(FieldType field) {
		return field.isPermanent() != null
				&& field.isPermanent().booleanValue();
	}

	protected LockFieldNameEnumeration mapLockField(LockOperation.Field field) {

		switch (field) {
		case ACCESS_PASSWORD:
			return LockFieldNameEnumeration.ACCESS_PWD;
		case KILL_PASSWORD:
			return LockFieldNameEnumeration.KILL_PWD;
		case EPC_MEMORY:
			return LockFieldNameEnumeration.EPC_BANK;
		case TID_MEMORY:
			return LockFieldNameEnumeration.TID_BANK;
		case USER_MEMORY:
			return LockFieldNameEnumeration.USER_BANK;
		default:
			return null;
		}
	}

	protected LockFieldNameEnumeration mapLockField(short bankIndex) {

		switch (bankIndex) {
		case RFUtils.BANK_EPC:
			return LockFieldNameEnumeration.EPC_BANK;
		case RFUtils.BANK_TID:
			return LockFieldNameEnumeration.TID_BANK;
		case RFUtils.BANK_USR:
			return LockFieldNameEnumeration.USER_BANK;
		default:
			return null;
		}
	}

	protected MemoryBankNameEnumeration mapBank(short bankIndex) {

		switch (bankIndex) {
		case RFUtils.BANK_EPC:
			return MemoryBankNameEnumeration.EPC_BANK;
		case RFUtils.BANK_TID:
			return MemoryBankNameEnumeration.TID_BANK;
		case RFUtils.BANK_PSW:
			return MemoryBankNameEnumeration.PWD_BANK;
		case RFUtils.BANK_USR:
			return MemoryBankNameEnumeration.USER_BANK;
		default:
			return null;
		}
	}

	protected String insert(String originalString, String stringToInsert,
			int offset) {
		if (originalString == null || originalString.length() == 0)
			originalString = "";

		/* aaaabbbb|NNNN|ccccdddd */
		/* lPart <-|NNNN|-> rPart */

		/* NNNNbbbbccccdddd => N.offset == 0: empty lPart */
		/*
		 * aaaaNNNNccccdddd => N.offset > 0 && N.offset+N.length < length: lPart
		 * and rPart
		 */
		/* aaaabbbbccccNNNN => N.offset + N.length >= length empty rPart */
		if (offset < originalString.length()) {

			String leftPart = "";
			String rightPart = "";

			if (offset > 0)
				leftPart = originalString.substring(0, offset);

			if (offset + stringToInsert.length() < originalString.length())
				rightPart = originalString.substring(offset
						+ stringToInsert.length());

			return leftPart + stringToInsert + rightPart;
		}

		/* N.offset >= length */
		/* aaaabbbbccccddddNNNN => append */
		/* aaaabbbbccccdddd0000NNNN => fill with 0 and append */
		else {
			String fillStr = "";
			for (int i = originalString.length(); i < offset; i++)
				fillStr += '0';
			return originalString + fillStr + stringToInsert;
		}
	}

	private void commit(RequestCreateTagType tag) {
		RequestUpdateType update = createUpdateRequest();
		update.setTags(new RequestCreateTagsType());
		update.getTags().getTag().add(tag);
		manager.process(createRequest(ID, update));
	}

	public List<RequestType> createRequest(String configId, RequestAbstractType choice) {
		List<RequestType> requests = new ArrayList<>();
		RequestType request = new RequestType();
		request.setConfigId(configId);
		request.getChoice().add(choice);
		requests.add(request);
		return requests;
	}

	public ResponseReadAntennasType getAntennas() {
		RequestReadType read = createReadRequest();
		read.setAntennas(new RequestDeleteAntennasType());
		List<ResponseType> resp = manager.process(createRequest(ID, read));
		ResponseReadAntennasType ret = ((ResponseReadType) resp.get(0)
				.getChoice().get(0)).getAntennas();
		return ret == null ? new ResponseReadAntennasType() : ret;
	}

	public RequestCreateType createCreateRequest() {
		RequestCreateType create = new RequestCreateType();
		create.setOperationId(createOperationId());
		return create;
	}

	public RequestUpdateType createUpdateRequest() {
		RequestUpdateType update = new RequestUpdateType();
		update.setOperationId(createOperationId());
		return update;
	}

	public RequestDeleteType createDeleteRequest() {
		RequestDeleteType delete = new RequestDeleteType();
		delete.setOperationId(createOperationId());
		return delete;
	}

	public RequestReadType createReadRequest() {
		RequestReadType read = new RequestReadType();
		read.setOperationId(createOperationId());
		return read;
	}

	private String createOperationId() {
		return String.valueOf(operationId++);
	}
}