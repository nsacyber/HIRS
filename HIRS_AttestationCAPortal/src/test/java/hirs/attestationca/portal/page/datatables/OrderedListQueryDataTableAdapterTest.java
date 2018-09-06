package hirs.attestationca.portal.page.datatables;

import hirs.FilteredRecordsList;
import hirs.data.persist.Device;
import hirs.persist.CriteriaModifier;
import hirs.persist.OrderedListQuerier;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.datatables.Search;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OrderedListQueryDataTableAdapter}.
 */
public class OrderedListQueryDataTableAdapterTest {

    private OrderedListQuerier<Device> querier;

    private FilteredRecordsList filteredList;

    @Captor
    private ArgumentCaptor<Map<String, Boolean>> captor;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setup() {

        // sets up the @Captor
        MockitoAnnotations.initMocks(this);

        querier = (OrderedListQuerier<Device>)
                mock(OrderedListQuerier.class);

        filteredList  = new FilteredRecordsList();

        when(querier.getOrderedList(Matchers.<Class<Device>>any(), anyString(), anyBoolean(),
            anyInt(), anyInt(), anyString(), anyMap(), any(CriteriaModifier.class)))
            .thenReturn(filteredList);
    }


    /**
     * Tests that a query passes the right arguments to a OrderedListQuerier via the adapter.
     */
    @Test
    public void getSimpleQuery() {

        final int startIndex = 50;
        final int length = 70;
        final int columnMapSize = 2;
        final String searchString = "AAAA_BBB";
        final DataTableInput dataTableInput = new DataTableInput();

        Order order = new Order(0, true);
        List<Order> orderList = new ArrayList<>();
        orderList.add(order);

        List<Column> searchColumns = new ArrayList<>();
        searchColumns.add(new Column("name", "name", true, true, new Search()));
        searchColumns.add(new Column("healthStatus", "healthStatus", true, true, new Search()));

        dataTableInput.setStart(startIndex);
        dataTableInput.setLength(length);
        dataTableInput.setOrder(orderList);
        dataTableInput.setSearch(new Search(searchString));
        dataTableInput.setColumns(searchColumns);

        FilteredRecordsList retrievedList =
                OrderedListQueryDataTableAdapter.getOrderedList(Device.class, querier,
                dataTableInput, "name");


        verify(querier, times(1)).getOrderedList(Matchers.<Class<Device>>any(),
                Matchers.eq("name"),
                Matchers.eq(true), Matchers.eq(startIndex), Matchers.eq(length),
                Matchers.eq(searchString), captor.capture(), any(CriteriaModifier.class));

        Assert.assertSame(retrievedList, filteredList);

        Assert.assertEquals(captor.getValue().size(), columnMapSize);
    }
}
