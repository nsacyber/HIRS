package hirs.utils;

import com.github.marandus.pciid.service.PciIdsDatabase;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provide Java access to PCI IDs.
 */
@Log4j2
public final class PciIds {

    /**
     * This pci ids file can be in different places on different distributions.
     */
    public static final List<String> PCI_IDS_PATH =
            Collections.unmodifiableList(new ArrayList<>() {
                private static final long serialVersionUID = 1L;
                {
                    add("/usr/share/hwdata/pci.ids");
                    add("/usr/share/misc/pci.ids");
                    add("/tmp/pci.ids");
                }
            });

    /**
     * The PCI IDs Database object.
     *
     * This only needs to be loaded one time.
     *
     * The pci ids library protects the data inside the object by making it immutable.
     */
    public static final PciIdsDatabase DB = new PciIdsDatabase();

    static {
        if (!DB.isReady()) {
            String dbFile = null;
            for (final String path : PCI_IDS_PATH) {
                if ((new File(path)).exists()) {
                    log.info("PCI IDs file was found {}", path);
                    dbFile = path;
                    break;
                }
            }
            if (dbFile != null) {
                InputStream is = null;
                try {
                    is = new FileInputStream(new File(dbFile));
                    DB.loadStream(is);
                } catch (IOException e) {
                    // DB will not be ready, hardware IDs will not be translated
                    dbFile = null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            dbFile = null;
                        }
                    }
                }
            }
        }
    }

}
