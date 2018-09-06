/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hirs.data.persist;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Transient;

/**
 * An <code>IMAIgnoreSetRecord</code> contains a filepath and description of
 * files that should be ignored in IMA reports.  The description is to provide
 * insight into why a file or group of files (in the case of dynamic matching) was ignored.
 *
 */
@Entity
public class ImaIgnoreSetRecord extends AbstractImaBaselineRecord {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ima_ignore_set_id")
    private ImaIgnoreSetBaseline baseline;

    @Transient
    private static final Pattern RECORD_PATTERN = Pattern.compile("\\((.*),.*\\)");

    /**
     * Creates a new <code>ImaIgnoreSetRecord</code>.
     *
     * @param path file path, not null
     */
    public ImaIgnoreSetRecord(final String path) {
        this(path, null, null);
    }

    /**
     * Creates a new <code>ImaIgnoreSetRecord</code>.
     *
     * @param path file path, not null
     * @param description description of why the file path was added to the ignore set, may be null
     */
    public ImaIgnoreSetRecord(final String path, final String description) {
        this(path, description, null);
    }

    /**
     * Creates a new <code>ImaIgnoreSetRecord</code>.
     *
     * @param path file path
     * @param baseline the IMA ignore set baseline this record belongs to, may be null
     */
    public ImaIgnoreSetRecord(final String path, final ImaIgnoreSetBaseline baseline) {
        this(path, null, baseline);
    }

    /**
     * Creates a new <code>ImaIgnoreSetRecord</code>.
     *
     * @param path file path, not null
     * @param description description of why the file path was added to the ignore set, may be null
     * @param baseline the IMA ignore set baseline this record belongs to, may be null
     */
    public ImaIgnoreSetRecord(
            final String path,
            final String description,
            final ImaIgnoreSetBaseline baseline) {
        super(path, null, description);
        Preconditions.checkNotNull(path, "Path cannot be null");
        this.baseline = baseline;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ImaIgnoreSetRecord() {
        super();
    }

    /**
     * This gets the baseline associated with the ignore set record.
     *
     * @return ImaIgnoreSetBaseline
     */
    public final ImaIgnoreSetBaseline getBaseline() {
        return baseline;
    }

    /**
     * Sets the given baseline.
     *
     * @param recordBaseline baseline that matches the given baseline
     */
    public final void setBaseline(final ImaIgnoreSetBaseline recordBaseline) {
        setOnlyBaseline(recordBaseline);
        if (recordBaseline != null) {
            recordBaseline.addOnlyToBaseline(this);
        }
    }

    /**
     * Sets the baseline for this record.
     *
     * @param baseline
     *            baseline or null
     */
    public final void setOnlyBaseline(final ImaIgnoreSetBaseline baseline) {
        if (this.baseline != null && baseline != null) {
            this.baseline.removeOnlyBaseline(this);
        }

        this.baseline = baseline;
    }

    /**
     * Designed to translate the 'received' String field in an <code>Alert</code> into an <code>
     * ImaIgnoreSetRecord</code>.  Throws an IllegalArgumentException if an invalid String is passed
     * in
     *
     * @param record        String formatted like the 'received' field of an <code>Alert</code>
     * @param description   Description to be provided for the IMA ignore set baseline record
     * @return ImaIgnoreSetRecord       built ImaIgnoreSetRecord based on report record String
     */
    public static ImaIgnoreSetRecord fromString(final String record, final String description) {
        Matcher m = RECORD_PATTERN.matcher(record);
        m.matches();

        //Verifies that one and only one group was captured based on the Regex pattern.
        if (m.groupCount() != 1) {
            String msg = String.format("Unexpected number of groups found with pattern \"%s\" "
                    + "on string \"%s\"", RECORD_PATTERN.toString(), record);

            throw new IllegalArgumentException(msg);

        }

        return new ImaIgnoreSetRecord(m.group(1), description);
    }
}
