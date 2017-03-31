package ch.hsr.ifs.cdttesting.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ch.hsr.ifs.cdttesting.test.cdttest.tests.NormalizerTest;

@RunWith(Suite.class)
//@formatter:off
@SuiteClasses({
	NormalizerTest.class
	})
public class TestSuiteAll {

}
