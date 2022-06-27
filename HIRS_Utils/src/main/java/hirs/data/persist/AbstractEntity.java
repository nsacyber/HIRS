package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Date;
import java.util.UUID;

/**
 * An abstract database entity.
 */
@MappedSuperclass
public abstract class AbstractEntity {

    /**
     * static value for the length of a status message for objects that
     * can have extremely long values, potentially.
     */
    protected static final int RESULT_MESSAGE_LENGTH = 1000000;

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Column (name = "create_time")
    private final Date createTime = new Date();

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected AbstractEntity() {
        super();
    }

    /**
     * Returns the unique ID associated with this entity.
     *
     * @return unique ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the creation time of this entity.
     *
     * @return creation time
     */
    public final Date getCreateTime() {
        return (Date) createTime.clone();
    }

    /**
     * Reset the creation time to the current time.
     */
    public final void resetCreateTime() {
        createTime.setTime(new Date().getTime());
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(this.getClass().equals(obj.getClass()))) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        try {
            return String.format("UUID=%s, createTime=%s",
                getId(), getCreateTime().toString());
        } catch (NullPointerException npEx) {
            return "";
        }
    }
}
