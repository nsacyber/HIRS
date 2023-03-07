package hirs.attestationca.persist.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * This is a class for persisting <code>X509Certificate</code> objects via
 * Hibernate. This class provides the mapping from <code>X509Certificate</code>
 * to Hibernate commands to JDBC.
 */
@NoArgsConstructor(access= AccessLevel.PUBLIC)
public final class X509CertificateType implements UserType {

    @Override
    public int getSqlType() {
        return Types.BLOB;
    }

    /**
     * Returns the <code>X509Certificate</code> class.
     *
     * @return <code>X509Certificate</code> class
     */
    @Override
    public Class returnedClass() {
        return X509Certificate.class;
    }

    /**
     * Compares x and y using {@link Objects#equals(Object, Object)}.
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
     * <code>X509Certificate</code>.
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
     * Converts the X509Certificate that is stored as a <code>String</code> and
     * converts it to an <code>X509Certificate</code>.
     *
     * @param rs
     *            result set
     * @param names
     *            column names
     * @param session
     *            session
     * @param owner
     *            owner
     * @return X509Certificate of String
     * @throws HibernateException
     *             if unable to convert the String to an X509Certificate
     * @throws SQLException
     *             if unable to retrieve the String from the result set
     */
    @Override
    public Object nullSafeGet(final ResultSet rs, final int names,
                              final SharedSessionContractImplementor session, final Object owner)
            throws HibernateException, SQLException {
        final Blob cert = rs.getBlob(names);
        if (cert == null) {
            return null;
        }
        try {
            InputStream inputStream = new ByteArrayInputStream(
                    cert.getBytes(1, (int) cert.length()));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificate(inputStream);
        } catch (CertificateException e) {
            final String msg = String.format(
                    "unable to convert certificate: %s", cert);
            throw new HibernateException(msg, e);
        }
    }

    /**
     * Converts the <code>X509Certificate</code> <code>value</code> to a
     * <code>String</code> and stores it in the database.
     *
     * @param st prepared statement
     * @param value X509Certificate
     * @param index index
     * @param session session
     * @throws SQLException if unable to set the value in the result set
     */
    @Override
    public void nullSafeSet(final PreparedStatement st, final Object value,
                            final int index, final SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setString(index, null);
        } else {
            try {
                Blob blob =
                        new SerialBlob(((Certificate) value).getEncoded());
                st.setBlob(index, blob);
            } catch (Exception e) {
                final String msg =
                        String.format("unable to convert certificate: %s",
                                value.toString());
                throw new HibernateException(msg, e);
            }
        }

    }

    /**
     * Returns <code>value</code> since <code>X509Certificate</code> is
     * immutable.
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
     * Returns false because <code>X509Certificate</code> is immutable.
     *
     * @return false
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Returns <code>value</code> because <code>X509Certificate</code> is
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
     * Returns <code>cached</code> because <code>X509Certificate</code> is
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
     * Returns the <code>original</code> because <code>X509Certificate</code> is
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
