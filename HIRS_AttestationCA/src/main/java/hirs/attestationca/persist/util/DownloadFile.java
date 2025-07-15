package hirs.attestationca.persist.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Basic POJO that encapsulates the information that is found in a downloaded file.
 */
@Getter
@Setter
@AllArgsConstructor
public class DownloadFile {
    /**
     * The name of the downloaded file.
     */
    private String fileName;

    /**
     * The content of the downloaded file as a byte array.
     */
    private byte[] fileBytes;
}
