package android.test;

import com.bt.BTInputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class BTInputStreamTest {
    private final static String TEST_READ_INCLUDING = "test read including";
    private final static String TEST_READ_INCLUDING_FOOTER = ";";
    private final static String TEST_READ_EXCLUDING = "test read excluding";
    private final static String TEST_READ_EXCLUDING_FOOTER = ";";
    private final static String TEST_SKIP_INCLUDING = "test skip including";
    private final static String TEST_SKIP_INCLUDING_FOOTER = ";";
    private final static String TEST_SKIP_INCLUDING_CONTINUATION = "value after skip including" + TEST_SKIP_INCLUDING_FOOTER;
    private final static String TEST_SKIP_EXCLUDING = "test skip excluding";
    private final static String TEST_SKIP_EXCLUDING_FOOTER = ";";
    private final static String TEST_SKIP_EXCLUDING_CONTINUATION_FOOTER = "%";
    private final static String TEST_SKIP_EXCLUDING_CONTINUATION = "value after skip excluding" + TEST_SKIP_EXCLUDING_CONTINUATION_FOOTER;
    private final static String TEST_READ = "test read";
    private final static String TEST_SKIP = "test skip";
    private final static String TEST_SKIP_CONTINUATION = "value after skip";

    private final BTInputStream input = new BTInputStream(
            new BTInputStreamTest.BTSimulator(
                    TEST_READ_INCLUDING + TEST_READ_INCLUDING_FOOTER
                            + TEST_READ_EXCLUDING + TEST_READ_EXCLUDING_FOOTER
                            + TEST_SKIP_INCLUDING + TEST_SKIP_INCLUDING_FOOTER + TEST_SKIP_INCLUDING_CONTINUATION
                            + TEST_SKIP_EXCLUDING + TEST_SKIP_EXCLUDING_FOOTER + TEST_SKIP_EXCLUDING_CONTINUATION
                            + TEST_READ
                            + TEST_SKIP + TEST_SKIP_CONTINUATION
            ),
            16
    );

    @Test
    public void readFromZeroTest() {
        try {
            Assert.assertEquals(TEST_READ_INCLUDING, input.read(TEST_READ_INCLUDING.length()));
        } catch (IOException e) {

        }
    }

    @Test
    public void readTest() {
        try {
            System.out.println("TEST READ INCLUDING");
            Assert.assertEquals(
                    TEST_READ_INCLUDING + TEST_READ_INCLUDING_FOOTER,
                    input.readIncluding(TEST_READ_INCLUDING_FOOTER.charAt(0))
            );

            System.out.println("TEST READ EXCLUDING");
            Assert.assertEquals(
                    TEST_READ_EXCLUDING,
                    input.readExcluding(TEST_READ_EXCLUDING_FOOTER.charAt(0))
            );
            //Skip footer as it's a part of this test
            input.skip(1);
        } catch (IOException e) {

        }
    }

    @Test
    public void skipTest(){
        try {
            readTest();
            System.out.println("TEST SKIP INCLUDING");
            input.skipIncluding(TEST_SKIP_INCLUDING_FOOTER.charAt(0));
            Assert.assertEquals(
                    TEST_SKIP_INCLUDING_CONTINUATION,
                    input.readIncluding(TEST_SKIP_INCLUDING_FOOTER.charAt(0))
            );

            System.out.println("TEST SKIP EXCLUDING");
            input.skipIncluding(TEST_SKIP_EXCLUDING_FOOTER.charAt(0));
            Assert.assertEquals(
                    TEST_SKIP_EXCLUDING_CONTINUATION,
                    input.readIncluding(TEST_SKIP_EXCLUDING_CONTINUATION_FOOTER.charAt(0))
            );
        }catch (IOException e){

        }
    }

    @Test
    public void basicReadSkipTest(){
        try{
            skipTest();
            System.out.println("TEST READ");
            Assert.assertEquals(
                    TEST_READ,
                    input.read(TEST_READ.length())
            );

            System.out.println("TEST SKIP");
            input.skip(TEST_SKIP.length());
            Assert.assertEquals(
                    TEST_SKIP_CONTINUATION,
                    input.read(TEST_SKIP_CONTINUATION.length())
            );
        }catch (IOException e){

        }
    }

    private static class BTSimulator extends InputStream {
        private final String mData;
        private int mCursor = 0;
        private final Random mRandomizer = new Random();

        public BTSimulator(String data) {
            this.mData = data;
        }

        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int numRead = mRandomizer.nextInt(b.length - 1) + 1;
            for (int i = 0; i < numRead; i++) {
                b[i] = (byte) mData.charAt(mCursor);
                mCursor++;
                if (mCursor >= mData.length()) {
                    mCursor = 0;
                }
            }
            return numRead;
        }
    }
}
