/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hirs.data.persist;

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
