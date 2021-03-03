package havis.device.test.rf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

public class RFCErrorTest {

	@Test
	public void testRfcErrorToReadResult() {
		assertEquals(ReadResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToReadResult(RFCError.NonSpecificTagError));
		assertEquals(ReadResult.Result.NO_RESPONSE_FROM_TAG, RFCError.rfcErrorToReadResult(RFCError.NoResponseFromTagError));
		assertEquals(ReadResult.Result.NON_SPECIFIC_READER_ERROR, RFCError.rfcErrorToReadResult(RFCError.NonSpecificReaderError));
		assertEquals(ReadResult.Result.MEMORY_OVERRUN_ERROR, RFCError.rfcErrorToReadResult(RFCError.MemoryOverrunError));
		assertEquals(ReadResult.Result.MEMORY_LOCKED_ERROR, RFCError.rfcErrorToReadResult(RFCError.MemoryLockedError));
		assertEquals(ReadResult.Result.INCORRECT_PASSWORD_ERROR, RFCError.rfcErrorToReadResult(RFCError.IncorrectPasswordError));
		
		assertEquals(ReadResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToReadResult(RFCError.ZeroKillPasswordError));
	}

	@Test
	public void testRfcErrorToWriteResult() {
		assertEquals(WriteResult.Result.MEMORY_OVERRUN_ERROR, RFCError.rfcErrorToWriteResult(RFCError.MemoryOverrunError));
		assertEquals(WriteResult.Result.MEMORY_LOCKED_ERROR, RFCError.rfcErrorToWriteResult(RFCError.MemoryLockedError));
		assertEquals(WriteResult.Result.INSUFFICIENT_POWER, RFCError.rfcErrorToWriteResult(RFCError.InsufficientPowerError));
		assertEquals(WriteResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToWriteResult(RFCError.NonSpecificTagError));
		assertEquals(WriteResult.Result.NO_RESPONSE_FROM_TAG, RFCError.rfcErrorToWriteResult(RFCError.NoResponseFromTagError));
		assertEquals(WriteResult.Result.NON_SPECIFIC_READER_ERROR, RFCError.rfcErrorToWriteResult(RFCError.NonSpecificReaderError));
		assertEquals(WriteResult.Result.INCORRECT_PASSWORD_ERROR, RFCError.rfcErrorToWriteResult(RFCError.IncorrectPasswordError));
		
		assertEquals(WriteResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToWriteResult(RFCError.ZeroKillPasswordError));
	}

	@Test
	public void testRfcErrorToLockResult() {
		assertEquals(LockResult.Result.INSUFFICIENT_POWER, RFCError.rfcErrorToLockResult(RFCError.InsufficientPowerError));
		assertEquals(LockResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToLockResult(RFCError.NonSpecificTagError));
		assertEquals(LockResult.Result.NO_RESPONSE_FROM_TAG, RFCError.rfcErrorToLockResult(RFCError.NoResponseFromTagError));
		assertEquals(LockResult.Result.NON_SPECIFIC_READER_ERROR, RFCError.rfcErrorToLockResult(RFCError.NonSpecificReaderError));
		assertEquals(LockResult.Result.INCORRECT_PASSWORD_ERROR, RFCError.rfcErrorToLockResult(RFCError.IncorrectPasswordError));
		assertEquals(LockResult.Result.MEMORY_OVERRUN_ERROR, RFCError.rfcErrorToLockResult(RFCError.MemoryOverrunError));
		assertEquals(LockResult.Result.MEMORY_LOCKED_ERROR, RFCError.rfcErrorToLockResult(RFCError.MemoryLockedError));
		
		assertEquals(LockResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToLockResult(RFCError.ZeroKillPasswordError));
	}

	@Test
	public void testRfcErrorToKillResult() {		
		assertEquals(KillResult.Result.ZERO_KILL_PASSWORD_ERROR, RFCError.rfcErrorToKillResult(RFCError.ZeroKillPasswordError));
		assertEquals(KillResult.Result.INSUFFICIENT_POWER, RFCError.rfcErrorToKillResult(RFCError.InsufficientPowerError));
		assertEquals(KillResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToKillResult(RFCError.NonSpecificTagError));
		assertEquals(KillResult.Result.NO_RESPONSE_FROM_TAG, RFCError.rfcErrorToKillResult(RFCError.NoResponseFromTagError));
		assertEquals(KillResult.Result.NON_SPECIFIC_READER_ERROR, RFCError.rfcErrorToKillResult(RFCError.NonSpecificReaderError));
		assertEquals(KillResult.Result.INCORRECT_PASSWORD_ERROR, RFCError.rfcErrorToKillResult(RFCError.IncorrectPasswordError));
		
		assertEquals(KillResult.Result.NON_SPECIFIC_TAG_ERROR, RFCError.rfcErrorToKillResult(RFCError.MemoryOverrunError));
	}

}
