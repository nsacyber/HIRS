package hirs.attestationca.portal.persist.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * This is a class for persisting <code>InetAddress</code> objects via
 * Hibernate. This class provides the mapping from <code>InetAddress</code> to
 * Hibernate commands to JDBC.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public final class InetAddressType implements UserType {

    /**
     * Returns varchar type.
     *
     * @return varchar type
     */
    @Override
    public int getSqlType() {
        return new StringJavaType.INSTANCE.sqlType();
    }

    /**
     * Returns the <code>InetAddress</code> class.
     *
     * @return <code>InetAddress</code> class
     */
    @Override
    public Class returnedClass() {
        return InetAddress.class;
    }

    /**
     * Compares x and y using {@link java.util.Objects#equals(Object, Object)}.
     *
     * @param x x
     * @param y y
     * @return value from equals call
     */
    @Override
    public boolean equals(final Object x, final Object y) {
        return Objects.equals(x, y);
    }

    /**
     * Returns the hash code of x, which will be the same as from
     * <code>InetAddress</code>.
     *
     * @param x x
     * @return hash value of x
     */
    @Override
    public int hashCode(final Object x) {
        assert x != null;
        return x.hashCode();
    }

    /**
     * Converts the IP address that is stored as a <code>String</code> and
     * converts it to an <code>InetAddress</code>.
     *
     * @param rs
     *            result set
     * @param names
     *            column names
     * @param session
     *            session
     * @param owner
     *            owner
     * @return InetAddress of String
     * @throws HibernateException
     *             if unable to convert the String to an InetAddress
     * @throws java.sql.SQLException
     *             if unable to retrieve the String from the result set
     */
    @Override
    public Object nullSafeGet(final ResultSet rs, final String[] names,
                              final SharedSessionContractImplementor session, final Object owner)
            throws HibernateException, SQLException {

        final String ip = StringJavaType.INSTANCE.getReplacement(rs.toString(), names[0],
                session);
        if (ip == null) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            final String msg = String.format("unable to convert ip address: %s", ip);
            throw new HibernateException(msg, e);
        }
    }

    /**
     * Converts the <code>InetAddress</code> <code>value</code> to a
     * <code>String</code> and stores it in the database.
     *
     * @param st prepared statement
     * @param value InetAddress
     * @param index index
     * @param session session
     * @throws java.sql.SQLException if unable to set the value in the result set
     */
    @Override
    public void nullSafeSet(final PreparedStatement st, final Object value,
                            final int index, final SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            StringJavaType.INSTANCE.set(st, null, index, session);
        } else {
            final InetAddress address = (InetAddress) value;
            final String ip = address.getHostAddress();
            StringJavaType.INSTANCE.set(st, ip, index, session);
        }
    }

    /**
     * Returns <code>value</code> since <code>InetAddress</code> is immutable.
     *
     * @param value value
     * @return value
     * @throws HibernateException will never be thrown
     */
    @Override
    public Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    /**
     * Returns false because <code>InetAddress</code> is immutable.
     *
     * @return false
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Returns <code>value</code> because <code>InetAddress</code> is
     * immutable.
     *
     * @param value value
     * @return value
     */
    @Override
    public Serializable disassemble(final Object value) {
        return (Serializable) value;
    }

    /**
     * Returns <code>cached</code> because <code>InetAddress</code> is
     * immutable.
     *
     * @param cached cached
     * @param owner owner
     * @return cached
     */
    @Override
    public Object assemble(final Serializable cached, final Object owner) {
        return cached;
    }

    /**
     * Returns the <code>original</code> because <code>InetAddress</code> is
     * immutable.
     *
     * @param original original
     * @param target target
     * @param owner owner
     * @return original
     */
    @Override
    public Object replace(final Object original, final Object target,
                          final Object owner) {
        return original;
    }
}
