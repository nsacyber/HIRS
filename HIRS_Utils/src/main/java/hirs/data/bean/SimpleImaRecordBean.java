package hirs.data.bean;

import hirs.data.persist.Digest;

/**
 * Provides a bean that can be used to encapsulate simple IMA record data.  SimpleImaRecordBean is
 * used to limit the IMA record result set when querying the database.  Instead of returning the
 * entire IMA record object, only a select set of fields are retrieved, the fields that comprise the
 * SimpleImaRecordBean.
 */
public class SimpleImaRecordBean {
    private Digest hash;
    private String path;

    /**
     * Get the digest.
     * @return Digest.
     */
    public Digest getHash() {
        return hash;
    }

    /**
     * Get the file path.
     * @return String.
     */
    public String getPath() {
        return path;
    }
}
