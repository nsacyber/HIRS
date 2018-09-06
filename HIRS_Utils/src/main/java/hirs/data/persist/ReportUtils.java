package hirs.data.persist;

import hirs.ReportRequest;

import java.util.ArrayList;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.reflections.Reflections;

/**
 * <code>ReportUtils</code> holds the utility methods used for
 * <code>Report</code>s.
 */
public final class ReportUtils {

    /**
     * Default constructor.
     */
    protected ReportUtils() {
        /* do nothing, not used */
    }

    /**
     * Builds a JAXBContext knowledgeable of all <code>Report</code> classes.
     * @return the JAXBContext
     * @throws JAXBException if a JAXB error occurs
     */
    public static JAXBContext getJAXBReportContext() throws JAXBException {
        ArrayList<Class<? extends Report>> classesList = new ArrayList<>();
        classesList.addAll(ReportUtils.getReportTypes("hirs"));
        JAXBContext context = JAXBContext.newInstance(classesList
                .toArray(new Class[classesList.size()]));
        return context;
    }

    /**
     * Builds a JAXBContext knowledgeable of all <code>ReportRequest</code> classes.
     * @return the JAXBContext
     * @throws JAXBException if a JAXB error occurs
     */
    public static JAXBContext getJAXBReportRequestTypeContext() throws JAXBException {
        ArrayList<Class<? extends ReportRequest>> classesList = new ArrayList<>();
        classesList.addAll(ReportUtils.getReportRequestTypes("hirs"));
        JAXBContext reportRequestContext = JAXBContext.newInstance(classesList
                .toArray(new Class[classesList.size()]));
        return reportRequestContext;
    }

    /**
     * This helper method returns a dynamic list of all subtypes of
     * <code>Report</code>s, (e.g., IntegrityReport.class,
     * DeviceInfoReport.class). This is developed to use with the JAXB Context
     * objects to allow dynamic loading of XML objects. For example:
     *
     * ArrayList&lt;Class&lt;? extends Report&gt;&gt; classList =
     *     ReportUtils.getReportTypes("hirs.data.persist");
     * JAXBContext context = JAXBContext.newInstance(classList.toArray(
     *     new Class[classList.size()]));
     *
     * @param packageName
     *            String package name, e.g. "hirs"
     * @return an ArrayList containing the relevant classes
     */
    public static ArrayList<Class<? extends Report>>
            getReportTypes(final String packageName) {
        ArrayList<Class<? extends Report>> list =
                new ArrayList<>();
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Report>> classes =
                reflections.getSubTypesOf(Report.class);
        for (Class<? extends Report> c : classes) {
            list.add(c);
        }
        return list;
    }

    /**
     * This helper method returns a dynamic list of all subtypes of
     * <code>ReportRequest</code>s, (e.g., IntegrityReport.class,
     * DeviceInfoReport.class). This is developed to use with the JAXB Context
     * objects to allow dynamic loading of XML objects. For example:
     *
     * ArrayList&lt;Class&lt;? extends Report&gt;&gt; classList =
     *     ReportUtils.getReportRequestTypes("hirs.data.persist");
     * JAXBContext context = JAXBContext.newInstance(classList.toArray(
     *     new Class[classList.size()]));
     *
     * @param packageName
     *            String package name, e.g. "hirs"
     * @return an ArrayList containing the relevant classes
     */
    public static ArrayList<Class<? extends ReportRequest>>
            getReportRequestTypes(final String packageName) {
        ArrayList<Class<? extends ReportRequest>> list = new ArrayList<>();
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends ReportRequest>> classes =
        reflections.getSubTypesOf(ReportRequest.class);
        for (Class<? extends ReportRequest> c : classes) {
            list.add(c);
        }
        return list;
    }
}
