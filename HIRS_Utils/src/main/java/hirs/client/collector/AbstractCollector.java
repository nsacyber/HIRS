package hirs.client.collector;

import hirs.ReportRequest;
import hirs.collector.Collector;
import hirs.collector.CollectorException;
import hirs.data.persist.Report;
import org.springframework.util.Assert;

/**
 * Base implementation of the <code>Collector</code> interface.
 */
public abstract class AbstractCollector implements Collector {

    @Override
    public final Report collect(final ReportRequest reportRequest) throws CollectorException {
        // ensure that there was a supplied report request and that this collector supports
        // collection of that type.
        Assert.notNull(reportRequest, "Cannot collect against a null report request");
        Assert.isTrue(reportRequestTypeSupported().isAssignableFrom(reportRequest.getClass()),
                String.format("%s collector does not support %s request types",
                        getClass().getSimpleName(), reportRequest.getClass().getSimpleName()));

        return doCollect(reportRequest);
    }

    /**
     * Performs the actual collection for the report request.
     *
     * @param reportRequest to collect
     * @return the collected report
     * @throws CollectorException when errors are encountered during collection.
     */
    abstract Report doCollect(ReportRequest reportRequest) throws CollectorException;

}
