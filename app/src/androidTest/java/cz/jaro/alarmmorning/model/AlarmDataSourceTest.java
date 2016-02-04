package cz.jaro.alarmmorning.model;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

public class AlarmDataSource2Test extends AndroidTestCase {

    private AlarmDataSource dataSource;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        dataSource = new AlarmDataSource(context);
        dataSource.open();
    }

    @Override
    public void tearDown() throws Exception {
        dataSource.close();
        super.tearDown();
    }

    public void testPreConditions() {
        assertNotNull(dataSource);
    }

    public void testVersion() {
        assertEquals(1, dataSource.version());
    }

    public void testVersion2() {
        assertEquals(3, dataSource.version());
    }

}
