package hirs.data.persist.baseline;

import java.util.List;

/**
 *
 */
public interface HasBaselines {
    /**
     * Convenience method for accessing related Baselines.
     * @return Baselines related to this object
     */
    List<Baseline> getBaselines();
}
