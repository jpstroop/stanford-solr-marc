package org.solrmarc.testUtils;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.Test;
import org.marc4j.MarcWriter;
import org.marc4j.marc.*;
import org.marc4j.marc.impl.DataFieldImpl;
import org.solrmarc.marc.RawRecordReader;
import org.solrmarc.marcoverride.MarcSplitStreamWriter;
import org.solrmarc.tools.RawRecord;

/**
 * Methods to assert when Record objects are equal or not, etc.
 * @author naomi
 *
 */
public class RecordTestingUtils 
{
    private static String testDir = "test";
    private static String testDataParentPath = System.getProperty("test.data.path", /*default to */testDir + File.separator + "data");
    private static String smokeTestDir = testDataParentPath + File.separator + "smoketest";
    private static String testConfigFile = System.getProperty("test.config.file", /*default to */smokeTestDir + File.separator + "test_config.properties");
 //   private static String testConfigFile = smokeTestDir + File.separator + testConfigFname;

    private static final String MARC_PRINTER_CLASS_NAME = "org.solrmarc.marc.MarcPrinter";
    private static final String MAIN_METHOD_NAME = "main";

	/**
	 * assert two Record objects are equal by comparing them as strings
	 */
	public static void assertEquals(Record expected, Record actual)
	{
		String actualId = actual.getControlNumber();
		String errmsg = "Record " + actualId + " wasn't as expected";
	    
	    if ( actualId.equals(expected.getControlNumber()) )
	    	assertTrue(errmsg, expected.toString().equals(actual.toString()) );
	    else
	    	fail(errmsg);
	}

	/**
	 * assert two Record objects aren't equal by comparing them as strings
	 */
	public static void assertNotEqual(Record expected, Record actual)
	{
		String actualId = actual.getControlNumber();
	    if ( !actualId.equals(expected.getControlNumber()) )
	    	return;
	
	    assertFalse("Records unexpectedly the same: " + actualId, expected.toString().equals(actual.toString()) );
	}

	/**
	 * assert two Record objects are equal by comparing them as strings, skipping over the leader
	 */
	public static void assertEqualsIgnoreLeader(Record expected, Record actual)
	{
		String actualId = actual.getControlNumber();
		String errmsg = "Record " + actualId + " wasn't as expected";
	    
	    if ( actualId.equals(expected.getControlNumber()) )
	    	assertTrue(errmsg, expected.toString().substring(24).equals(actual.toString().substring(24)) );
	    else
	    	fail(errmsg);
	}

	/**
	 * assert two Record objects are not equal by comparing them as strings, skipping over the leader
	 */
	public static void assertNotEqualIgnoreLeader(Record expected, Record actual)
	{
		String actualId = actual.getControlNumber();
	    if ( !actualId.equals(expected.getControlNumber()) )
	    	return;

	    assertFalse("Records unexpectedly the same: " + actualId, expected.toString().substring(24).equals(actual.toString().substring(24)) );
	}

	/**
	 * assert two RawRecord objects are equal 
	 *  First convert them to Record objects, assuming MARC8 encoding using permissive conversion, 
	 *  not converting them to utf8, combining 999 partials, and assuming
	 */
	public static void assertEquals(RawRecord expected, RawRecord actual, String encoding)
	{
		assertEquals(expected.getAsRecord(true,false, "999", encoding), actual.getAsRecord(true,false, "999", encoding));
	}

	/**
	 * assert two RawRecord objects are not equal by comparing them as byte[]
	 *  First convert them to Record objects, assuming MARC8 encoding using permissive conversion, 
	 *  not converting them to utf8, combining 999 partials, and assuming
	 */
	public static void assertNotEqual(RawRecord expected, RawRecord actual, String encoding)
	{
		assertNotEqual(expected.getAsRecord(true,false, "999", encoding), actual.getAsRecord(true,false, "999", encoding));
	}

    /**
     * compare two marc records;  the expected result is represented as
     *  an array of strings.  The leaders don't match; not sure why or if it
     *  matters.
     * @param expected
     * @param actual
     */
    public static void assertEqualsIgnoreLeader(String[] expected, Record actual) 
    {
    	String actualAsStr = actual.toString();
     	// removing leader is removing "LEADER " and the 24 char leader and the newline
    	String actualAsStrWithoutLdr = actualAsStr.substring(32);

     	StringBuffer buf = new StringBuffer();
    	for (int i = 1; i < expected.length; i++) {
    		buf.append(expected[i] + "\n");
    	}
    	
    	junit.framework.Assert.assertEquals("Records weren't equal", buf.toString(), actualAsStrWithoutLdr);
    }

	/**
	 * Given an expected marc record as an Array of strings corresponding to 
	 *  the lines in the output of MarcPrinter and 
	 * given the actual marc record as a ByteArrayOutputStream,
	 *  assert they are equal
	 */
	public static void assertMarcRecsEqual(String[] expectedAsLines, ByteArrayOutputStream actualAsBAOS)
	{
	    // convert actual record into an array of strings from MarcPrinter output
	    ByteArrayInputStream mergedMarcBibRecAsInStream = new ByteArrayInputStream(actualAsBAOS.toByteArray());
	    ByteArrayOutputStream marcPrinterOutputOfMergedBibRec = new ByteArrayOutputStream();
	    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
	    String[] marcPrintArgs = new String[]{testConfigFile, "print"};
	    CommandLineUtils.runCommandLineUtil(MARC_PRINTER_CLASS_NAME, MAIN_METHOD_NAME, mergedMarcBibRecAsInStream, marcPrinterOutputOfMergedBibRec, errorStream, marcPrintArgs); 
	
	    // did the resulting merged record contain the expected output?
	    assertMarcRecsEqual(expectedAsLines, new ByteArrayInputStream(marcPrinterOutputOfMergedBibRec.toByteArray()));
	}

	public static void assertMarcRecsEqual(String[] expectedAsLines, ByteArrayOutputStream actualAsBAOS, boolean ignoreLeader)
	{
		assertMarcRecsEqual(expectedAsLines, actualAsBAOS, ignoreLeader);
	}

	public static void assertMarcRecsEqual(String[] expectedAsLines, InputStream actualAsInputStream)
	{
		assertMarcRecsEqual(expectedAsLines, actualAsInputStream, false);
	}
	
	/**
	 * Given an expected marc record as an Array of strings corresponding to 
	 *  the lines in the output of MarcPrinter and given the actual marc record as an InputStream,
	 *  assert they are equal
	 */
	public static void assertMarcRecsEqual(String[] expectedAsLines, InputStream actualAsInputStream, boolean ignoreLeader) 
	{
	    BufferedReader actualAsBuffRdr = null;
	    try
	    {
	        actualAsBuffRdr = new BufferedReader(new InputStreamReader(actualAsInputStream, "UTF-8"));
	    }
	    catch (UnsupportedEncodingException e)
	    {
	        e.printStackTrace();
	        fail("couldn't read record to be tested from InputStream");
	    }
	
	    int numExpectedLines = expectedAsLines.length;
	
	    try
	    {
	        int lineCnt = 0;
	        String actualLine = null;
	        while ((actualLine = actualAsBuffRdr.readLine()) != null)
	        {
	            if (actualLine.length() == 0) 
	            {
	            	// do nothing;
	            }
	            else if (numExpectedLines > 0 && lineCnt < numExpectedLines) 
	            {
	                if (actualLine.equals("Flushing results...") || actualLine.equals("Flushing results done") || actualLine.startsWith("Cobertura:"))
	                    continue;   // skip this line and don't even count it.  I don't know where these "Flushing Results..." lines are coming from.
	
	                if (ignoreLeader && lineCnt == 0)
	                	continue;
	
	                String expectedLine = expectedAsLines[lineCnt];
	                junit.framework.Assert.assertEquals("output line ["+ actualLine + "]  doesn't match expected [" + expectedLine + "]", expectedLine, actualLine);
	            }
	            lineCnt++;
	        }
	    }
	    catch (IOException e)
	    {
	        e.printStackTrace();
	        fail("couldn't compare records");
	    }
	}

	/**
	 * Assert that each instance of the subfield is in the expected values
	 *  and that the number of instances match.
	 */
	public static void assertSubfieldHasExpectedValues(Record record, String fieldTag, char subfieldCode, Set<String> expectedVals)
	{
	    List<VariableField> vfList = record.getVariableFields(fieldTag);
	    Set<String> resultSet = new LinkedHashSet<String>();
	    for (VariableField vf : vfList )
	    {
	    	DataField df = (DataField) vf;
	    	List<Subfield> sfList = df.getSubfields(subfieldCode);
	    	for (Subfield sf : sfList) 
	    	{
	    		String val = sf.getData();
	    		resultSet.add(val);
    			assertTrue("Got unexpected value " + val, expectedVals.contains(val));
			}
	    }
	    org.junit.Assert.assertEquals("Number of values doesn't match", expectedVals.size(), resultSet.size());
	}
	
	/**
	 * Assert that no instances of the subfield have the unexpected values
	 */
	public static void assertSubfieldDoesNotHaveValues(Record record, String fieldTag, char subfieldCode, Set<String> unexpectedVals)
	{
		int count = 0;
	    List<VariableField> vfList = record.getVariableFields(fieldTag);
	    for (VariableField vf : vfList)
	    {
	    	DataField df = (DataField) vf;
	    	List<Subfield> sfList = df.getSubfields(subfieldCode);
	    	for (Subfield sf : sfList) 
	    	{
	    		String val = sf.getData();
	    		count = count + 1;
    			assertFalse("Got unexpected value " + val, unexpectedVals.contains(val));
			}
	    }
	}
	
	
	/**
	 * convert a Record object to a RawRecord object.  
	 * Uses MarcSplitStreamWriter to output the record so it can be read in again.
	 */
	public static RawRecord convertToRawRecord(Record record)
	{
	    // prepare to trap MarcWriter output stream 
		ByteArrayOutputStream sysBAOS = TestingUtil.getSysMsgsBAOS();
		
		MarcWriter writer = new MarcSplitStreamWriter(System.out, "ISO-8859-1", 70000, "999");
	    writer.write(record);
	    System.out.flush();
		
		ByteArrayInputStream recAsInStream = new ByteArrayInputStream(sysBAOS.toByteArray());
		
		return new RawRecord(new DataInputStream((InputStream) recAsInStream));
	}
	
	/**
	 * given a file of records as a ByteArrayOutputStream and a record id,
	 *  look for that record.  If it is found, return it as a RawRecord object,
	 *  otherwise, return null
	 */
// FIXME: not used? tested, even?
	public static RawRecord extractRecord(ByteArrayOutputStream recsFileAsBAOutStream, String recId)
	{
	    ByteArrayInputStream fileAsInputStream = new ByteArrayInputStream(recsFileAsBAOutStream.toByteArray());
		RawRecordReader fileRawRecReader = new RawRecordReader(fileAsInputStream);    	
	    while (fileRawRecReader.hasNext())
	    {
	        RawRecord rawRec = fileRawRecReader.next();
	        if (recId == rawRec.getRecordId())
	        	return rawRec;
	    }
	    return null;
	}

	
// Tests for assertion methods ------------------------------------------------	
	
	
	/**
	 * ensure that the assertEquals and assertNotEqual methods work for 
	 *  RawRecord objects
	 */
@Test
    public static void testRawRecordAssertEqualsAndNot()
  		throws IOException
  	{
  	    String bibRecFileName = testDataParentPath + File.separator + "u335.mrc";

  	    RawRecordReader bibsRawRecRdr = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
  	    if (bibsRawRecRdr.hasNext()) {
  	    	
  	        RawRecord rawRec1 = bibsRawRecRdr.next();
  	        assertEquals(rawRec1, rawRec1, "MARC8");
  	        
  	        RawRecordReader bibsRawRecRdr2 = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
  	        if (bibsRawRecRdr2.hasNext()) {
  	  	        RawRecord rawRec2 = bibsRawRecRdr2.next();
  	  	        Record rec2 = rawRec2.getAsRecord(true, false, "999", "MARC8");
  	  	        DataField dataFld = new DataFieldImpl("333", ' ', ' ');
  	  	        rec2.addVariableField(dataFld);
  	  	        assertNotEqual(rawRec1, convertToRawRecord(rec2), "MARC8");
  	        }
  	  	    else
  	            fail("shouldn't get here");
  	    }
  	    else
            fail("shouldn't get here");
  	}

	/**
	 * ensure that the assertEquals and assertNotEqual methods work for 
	 *  Record objects
	 */
@Test  	
  	public static void testRecordAssertEqualsAndNot()
  	  		throws IOException
  	{
  	    String bibRecFileName = testDataParentPath + File.separator + "u335.mrc";

  	    RawRecordReader bibsRawRecRdr = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
  	    if (bibsRawRecRdr.hasNext()) 
  	    {
  	        RawRecord rawRec1 = bibsRawRecRdr.next();
  	        Record rec1 = rawRec1.getAsRecord(true, false, "999", "MARC8");

  	        assertEquals(rec1, rec1);
  	        
  	        RawRecordReader bibsRawRecRdr2 = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
  	        if (bibsRawRecRdr2.hasNext())
  	        {
  	  	        RawRecord rawRec2 = bibsRawRecRdr2.next();
  	  	        Record rec2 = rawRec2.getAsRecord(true, false, "999", "MARC8");
  	  	        DataField dataFld = new DataFieldImpl("333", ' ', ' ');
  	  	        rec2.addVariableField(dataFld);

  	  	        assertNotEqual(rec1, rec2);
  	        }
  	  	    else
  	            fail("shouldn't get here");
  	    }
  	    else
            fail("shouldn't get here");
  	}

	/**
	 * ensure that the assertEquals and assertNotEqual methods work for 
	 *  Record objects
	 */
@Test  	
	public static void testRecordAssertEqualsIgnoreLeaderAndNot()
	  		throws IOException
	{
	    String bibRecFileName = testDataParentPath + File.separator + "u335.mrc";

	    RawRecordReader bibsRawRecRdr = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
	    if (bibsRawRecRdr.hasNext()) 
	    {
	        RawRecord rawRec1 = bibsRawRecRdr.next();
	        Record rec1 = rawRec1.getAsRecord(true, false, "999", "MARC8");

	        assertEqualsIgnoreLeader(rec1, rec1);
	        
	        RawRecordReader bibsRawRecRdr2 = new RawRecordReader(new FileInputStream(new File(bibRecFileName)));
	        if (bibsRawRecRdr2.hasNext())
	        {
	  	        RawRecord rawRec2 = bibsRawRecRdr2.next();
	  	        Record rec2 = rawRec2.getAsRecord(true, false, "999", "MARC8");
	  	        DataField dataFld = new DataFieldImpl("333", ' ', ' ');
	  	        rec2.addVariableField(dataFld);

	  	        assertNotEqualIgnoreLeader(rec1, rec2);
	        }
	  	    else
	            fail("shouldn't get here");
	    }
	    else
        fail("shouldn't get here");
	}

public static void assertRecordsEquals(String message, Record rec1, Record rec2)
{
    int result = compareRecords(rec1, rec2);
    String messageMore = null;
    if (result == 1) message = "Control Fields are different between rec1 and rec2";
    else if (result == 2) message = "Subfields are different between rec1 and rec2";
    else if (result == 3) message = "One record has a DataField where another has a ControlField";
    else if (result == -1) message = "Done with one record but not the other";
    org.junit.Assert.assertEquals(message+" "+messageMore, 0, result);
}

public static void assertRecordIsSubset(String message, Record rec1, Record rec2)
{
    int result = compareRecords(rec1, rec2);
    String messageMore= null;
    if (result == 1) message = "Control Fields are different between rec1 and rec2";
    else if (result == 2) message = "Subfields are different between rec1 and rec2";
    else if (result == 3) message = "One record has a DataField where another has a ControlField";
    else if (result == 0) message = "Records are equal when they shouldn't be";
    org.junit.Assert.assertEquals(message+" "+messageMore, -1, result);
}

public static int compareRecords(Record rec1, Record rec2)
{
    List<VariableField> fields1 = (List<VariableField>)rec1.getVariableFields();
    List<VariableField> fields2 = (List<VariableField>)rec2.getVariableFields();
    Iterator<VariableField> iter1 = fields1.iterator();
    Iterator<VariableField> iter2 = fields2.iterator();
    while (iter1.hasNext() && iter2.hasNext())
    {
        VariableField f1 = iter1.next();
        VariableField f2 = iter2.next();
        if (f1 instanceof ControlField && f2 instanceof ControlField)
        {
            ControlField cf1 = (ControlField)f1;
            ControlField cf2 = (ControlField)f2;
            if (! cf1.getData().equals(cf2.getData()))  return(1);
        }
        else if (f1 instanceof DataField && f2 instanceof DataField)
        {
            DataField df1 = (DataField)f1;
            DataField df2 = (DataField)f2;
            List<Subfield> sfs1 = (List<Subfield>)df1.getSubfields();
            List<Subfield> sfs2 = (List<Subfield>)df2.getSubfields();
            Iterator<Subfield> iter3 = sfs1.iterator();
            Iterator<Subfield> iter4 = sfs2.iterator();
            while (iter3.hasNext() && iter4.hasNext())
            {
                Subfield sf1 = iter3.next();
                Subfield sf2 = iter4.next();
                if (! sf1.getData().equals(sf2.getData()))  
                    return(2);
            }
        }
        else 
        {
            return(3);
        }
    }
    // if done with one record but not the other
    if (iter1.hasNext() || iter2.hasNext())
    {
        return(-1);
    }
    return(0);
}
	/**
	 * Assign id of record to be the ckey. Our ckeys are in 001 subfield a. 
	 * Marc4j is unhappy with subfields in a control field so this is a kludge 
	 * work around.
	 */
	public static String getRecordIdFrom001(Record record)
	{
		String id = null;
		ControlField fld = (ControlField) record.getVariableField("001");
		if (fld != null && fld.getData() != null) 
		{
			String rawVal = fld.getData();
			// 'u' is for testing
			if (rawVal.startsWith("a") || rawVal.startsWith("u"))
				id = rawVal.substring(1);
		}
		return id;
	}

}
