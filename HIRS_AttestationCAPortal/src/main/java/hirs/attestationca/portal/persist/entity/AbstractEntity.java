package hirs.attestationca.portal.persist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * An abstract database entity.
 */
@ToString
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

    /**
     * static value for the length of a status message for objects that
     * can have extremely long values, potentially.
     */
    protected static final int RESULT_MESSAGE_LENGTH = 1000000;

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "uuid2", strategy=GenerationType.AUTO)
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Getter
    private UUID id;

    @Column (name = "create_time")
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    @Generated(GenerationTime.INSERT)
    private Date createTime;// = new Date();

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected AbstractEntity() {
        super();
    }

    /**
     * Setter for the UUID that can not be null
     * and can not be overridden.
     * @param id - primary able key
     */
    public void setId(UUID id) {
        if (id != null) {
            this.id = id;
        }
    }

    /**
     * Returns the creation time of this entity.
     *
     * @return creation time
     */
    public Date getCreateTime() {
        return (Date) createTime.clone();
    }

    /**
     * Reset the creation time to the current time.
     */
    public void resetCreateTime() {
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
}
