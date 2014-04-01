package com.jamesontriplett.app;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
	
	ScreenScraper ss;
    @Before
    public void init()
	   {
		   ss = new ScreenScraper();
	   }
	
	public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigorous Test :-)
     * @throws FileNotFoundException 
     */
    public void testApp() throws FileNotFoundException
    {
    	PrintStream out = new PrintStream("inputtest.csv");
    	out.println("Symbol");
    	out.println("FLT");
    	out.println("LL");
    	out.close();

    	String [] args = {"-input", "inputtest.csv"};
    	ScreenScraper.main(args);
        assertTrue( true );
    }
}
