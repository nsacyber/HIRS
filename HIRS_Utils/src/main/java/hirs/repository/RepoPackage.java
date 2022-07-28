package hirs.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.data.persist.Digest;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * This class represents a software package found in a repository.  It is identified by its name,
 * version, and target architecture, and can store measurements of its contents.  These measurements
 * are the listing of the package's files (referenced by absolute path) and their hashes, stored
 * as {@link Object}s.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames =
        { "name", "version", "packageRelease", "architecture", "sourceRepository" }
))
public abstract class RepoPackage {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column
    private String name;

    @Column
    private String version;

    // release is a reserved word in MySQL
    @Column(name = "packageRelease")
    private String release;

    @Column
    private String architecture;

//    @ManyToOne
//    @JoinColumn(name = "sourceRepository")
//    private Repository<?> sourceRepository;

    @Column
    private boolean measured = false;

    /**
     * Name of the packageRecords field.
     */
    public static final String PACKAGE_RECORDS_FIELD = "packageRecords";

    @Column(name = PACKAGE_RECORDS_FIELD)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Object> packageRecords = null;

    @Embedded
    private Digest packageMeasurement = null;

    @Column
    private Date measurementDate = null;

    /**
     * Constructs a RepoPackage.
     *
     * @param name the package's name
     * @param version the package's version
     * @param release the package's release
     * @param architecture the package's target architecture
     * @param sourceRepository the repository that this package was downloaded from
     */
    public RepoPackage(final String name, final String version, final String release,
                       final String architecture, final Repository sourceRepository) {
        if (name == null) {
            throw new IllegalArgumentException("Package name can't be null");
        }

        if (version == null) {
            throw new IllegalArgumentException("Package version can't be null");
        }

        if (release == null) {
            throw new IllegalArgumentException("Package release can't be null");
        }

        if (architecture == null) {
            throw new IllegalArgumentException("Package architecture can't be null");
        }

        if (sourceRepository == null) {
            throw new IllegalArgumentException("Source repository can't be null");
        }

        this.name = name;
        this.version = version;
        this.release = release;
        this.architecture = architecture;
//        this.sourceRepository = sourceRepository;
        this.id = UUID.randomUUID();
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected RepoPackage() {
    }

    /**
     * Gets the name of the package.
     *
     * @return the package's name
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the version of the package.
     *
     * @return the package's version
     */
    public final String getVersion() {
        return version;
    }


    /**
     * Gets the release of the package.
     *
     * @return the package's release
     */
    public final String getRelease() {
        return release;
    }

    /**
     * Gets the target architecture of the package.
     *
     * @return the package's target architecture
     */
    public final String getArchitecture() {
        return architecture;
    }

    /**
     * Gets the repository from which this package was downloaded and measured.
     *
     * @return this package's source repository
     */
//    public final Repository<?> getSourceRepository() {
//        return sourceRepository;
//    }

    /**
     * Sets the measurements of the package.  Must be only called on a package that has not yet
     * had its measurements set.
     *
     * @param packageRecords the measurements of the contents of the package
     * @param packageMeasurement the measurement of the package itself
     */
    public final void setAllMeasurements(final Set<Object> packageRecords,
                                         final Digest packageMeasurement) {
        if (packageRecords == null) {
            throw new IllegalArgumentException("Measurements cannot be null.");
        }

        if (packageMeasurement == null) {
            throw new IllegalArgumentException("Package measurement cannot be null.");
        }

        if (!measured) {
            this.packageRecords = new HashSet<>(packageRecords);
            this.packageMeasurement = packageMeasurement;
            measured = true;
            measurementDate = new Date();
        } else {
            throw new IllegalStateException("RepoPackage measurements already set.");
        }
    }

    /**
     * Gets the package's measurements.
     *
     * @return a set of {@link Object}s representing the measurements of the files in
     * this package
     */
    public final Set<Object> getPackageRecords() {
        if (!measured) {
            throw new IllegalStateException("Package measurements not yet set.");
        }

        return new HashSet<>(packageRecords);
    }

    /**
     * Gets the package's own measurement.
     *
     * @return the package's measurement
     */
    public final Digest getPackageMeasurement() {
        return packageMeasurement;
    }

    /**
     * Gets the status of the package's measurement.
     *
     * @return a boolean representing whether the package has been measured or not.
     */
    public final boolean isMeasured() {
        return measured;
    }

    /**
     * Gets the point in time when a package was measured.
     *
     * @return the Date when the package was measured or null if it has yet to be measured.
     */
    public final Date getMeasurementDate() {
        if (measurementDate == null) {
            return null;
        }

        return new Date(measurementDate.getTime());
    }

    /**
     * Gets the ID of this package.
     *
     * @return this package's ID
     */
    public final UUID getId() {
        return id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(o instanceof RepoPackage)) {
            return false;
        }

        RepoPackage that = (RepoPackage) o;

        if (!name.equals(that.name)) {
            return false;
        }

        if (!version.equals(that.version)) {
            return false;
        }

        if (!release.equals(that.release)) {
            return false;
        }

        if (!architecture.equals(that.architecture)) {
            return false;
        }

//        if (!sourceRepository.equals(that.sourceRepository)) {
//            return false;
//        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = name.hashCode();
        result = prime * result + version.hashCode();
        result = prime * result + release.hashCode();
        result = prime * result + architecture.hashCode();
        return result;
    }

    @Override
    public final String toString() {
        return String.format(
                "RepoPackage{name=%s, version=%s, release=%s, architecture=%s",
                name, version, release, architecture);
    }
}
