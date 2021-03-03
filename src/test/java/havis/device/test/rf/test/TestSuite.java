package havis.device.test.rf.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import havis.device.test.rf.RFCErrorTest;
import havis.device.test.rf.StubHardwareApiTest;
import havis.device.test.rf.StubHardwareManagerTest;

@RunWith(Suite.class)
@SuiteClasses({ StubHardwareManagerTest.class, StubHardwareApiTest.class, RFCErrorTest.class })
public class TestSuite {

}
