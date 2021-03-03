package havis.device.test.rf;

import havis.device.rf.tag.result.KillResult;
import havis.device.rf.tag.result.LockResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

public enum RFCError {

	NonSpecificTagError, NonSpecificReaderError, NoResponseFromTagError,
	MemoryOverrunError, MemoryLockedError, IncorrectPasswordError,
	InsufficientPowerError, ZeroKillPasswordError;
	
	protected static ReadResult.Result rfcErrorToReadResult(RFCError rfcErr)
	{
		switch (rfcErr)
		{
			case NonSpecificTagError: return ReadResult.Result.NON_SPECIFIC_TAG_ERROR;
			case NoResponseFromTagError: return ReadResult.Result.NO_RESPONSE_FROM_TAG;
			case NonSpecificReaderError: return ReadResult.Result.NON_SPECIFIC_READER_ERROR;
			case MemoryOverrunError: return ReadResult.Result.MEMORY_OVERRUN_ERROR;
			case MemoryLockedError: return ReadResult.Result.MEMORY_LOCKED_ERROR;
			case IncorrectPasswordError: return ReadResult.Result.INCORRECT_PASSWORD_ERROR;
			default: return ReadResult.Result.NON_SPECIFIC_TAG_ERROR;
		}
	}
	
	protected static WriteResult.Result rfcErrorToWriteResult(RFCError rfcErr)
	{
		switch (rfcErr)
		{
			case MemoryOverrunError: return WriteResult.Result.MEMORY_OVERRUN_ERROR;
			case MemoryLockedError: return WriteResult.Result.MEMORY_LOCKED_ERROR;
			case InsufficientPowerError: return WriteResult.Result.INSUFFICIENT_POWER;
			case NonSpecificTagError: return WriteResult.Result.NON_SPECIFIC_TAG_ERROR;
			case NoResponseFromTagError: return WriteResult.Result.NO_RESPONSE_FROM_TAG;		
			case NonSpecificReaderError: return WriteResult.Result.NON_SPECIFIC_READER_ERROR;
			case IncorrectPasswordError: return WriteResult.Result.INCORRECT_PASSWORD_ERROR;
			default: return WriteResult.Result.NON_SPECIFIC_TAG_ERROR;
		}		
	}
	
	protected static LockResult.Result rfcErrorToLockResult(RFCError rfcErr)
	{
		switch (rfcErr)
		{
			case InsufficientPowerError: return LockResult.Result.INSUFFICIENT_POWER;	
			case NonSpecificTagError: return LockResult.Result.NON_SPECIFIC_TAG_ERROR;
			case NoResponseFromTagError: return LockResult.Result.NO_RESPONSE_FROM_TAG;
			case NonSpecificReaderError: return LockResult.Result.NON_SPECIFIC_READER_ERROR;
			case IncorrectPasswordError: return LockResult.Result.INCORRECT_PASSWORD_ERROR; 
			case MemoryOverrunError: return LockResult.Result.MEMORY_OVERRUN_ERROR;
			case MemoryLockedError: return LockResult.Result.MEMORY_LOCKED_ERROR;
			default: return LockResult.Result.NON_SPECIFIC_TAG_ERROR;
		}
	}
	
	protected static KillResult.Result rfcErrorToKillResult(RFCError rfcErr)
	{
		switch (rfcErr)
		{
			case ZeroKillPasswordError: return KillResult.Result.ZERO_KILL_PASSWORD_ERROR;	
			case InsufficientPowerError: return KillResult.Result.INSUFFICIENT_POWER;
			case NonSpecificTagError: return KillResult.Result.NON_SPECIFIC_TAG_ERROR;
			case NoResponseFromTagError: return KillResult.Result.NO_RESPONSE_FROM_TAG;
			case NonSpecificReaderError: return KillResult.Result.NON_SPECIFIC_READER_ERROR; 
			case IncorrectPasswordError: return KillResult.Result.INCORRECT_PASSWORD_ERROR;
			default: return KillResult.Result.NON_SPECIFIC_TAG_ERROR;
		}
	}
	
}
