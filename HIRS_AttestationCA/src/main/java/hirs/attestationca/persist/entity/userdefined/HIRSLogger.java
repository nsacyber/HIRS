package hirs.attestationca.persist.entity.userdefined;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.logging.LogLevel;

/**
 * Custom-object representation of a HIRS-Application logger.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HIRSLogger {
    private String loggerName;
    private LogLevel logLevel;
}
