package org.broadinstitute.sting.gatk;

import net.sf.picard.filter.SamRecordFilter;
import net.sf.samtools.SAMFileReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
/**
 * User: hanna
 * Date: May 14, 2009
 * Time: 4:06:26 PM
 * BROAD INSTITUTE SOFTWARE COPYRIGHT NOTICE AND AGREEMENT
 * Software and documentation are copyright 2005 by the Broad Institute.
 * All rights are reserved.
 *
 * Users acknowledge that this software is supplied without any warranty or support.
 * The Broad Institute is not responsible for its use, misuse, or
 * functionality.
 */

/**
 * A data structure containing information about the reads data sources as well as
 * information about how they should be downsampled, sorted, and filtered.
 */
public class Reads {
    private List<File> readsFiles = null;
    private SAMFileReader.ValidationStringency validationStringency = SAMFileReader.ValidationStringency.STRICT;
    private Double downsamplingFraction = null;
    private Integer downsampleToCoverage = null;
    private Boolean beSafe = null;
    private Collection<SamRecordFilter> supplementalFilters = null;
    private int maximumReadsAtLocus = Integer.MAX_VALUE; // this should always be set, so we'll default it MAX_INT
    private boolean includeReadsWithDeletionAtLoci = false;


    /**
     * Return true if the walker wants to see reads that contain deletions when looking at locus pileups
     * 
     * @return
     */
    public boolean includeReadsWithDeletionAtLoci() {
        return includeReadsWithDeletionAtLoci;
    }

    /**
     * Gets a list of the files acting as sources of reads.
     * @return A list of files storing reads data.
     */
    public List<File> getReadsFiles() {
        return readsFiles;
    }

    /**
     * How strict should validation be?
     * @return Stringency of validation.
     */
    public SAMFileReader.ValidationStringency getValidationStringency() {
        return validationStringency;
    }

    /**
     * Get the fraction of reads to downsample.
     * @return Downsample fraction.
     */
    public Double getDownsamplingFraction() {
        return downsamplingFraction;
    }

    /**
     * Downsample each locus to the specified coverage.
     * @return Coverage to which to downsample.
     */
    public Integer getDownsampleToCoverage() {
        return downsampleToCoverage;
    }

    /**
     * get the maximum number of reads we allow at a locus for locus-by-hanger
     * @return the maximum reads allowed in a pile-up
     */
    public Integer getMaxReadsAtLocus() {
        return maximumReadsAtLocus;
    }

    /**
     * Return whether to 'verify' the reads as we pass through them.
     * @return Whether to verify the reads.
     */
    public Boolean getSafetyChecking() {
        return beSafe;
    }

    public Collection<SamRecordFilter> getSupplementalFilters() {
        return supplementalFilters;
    }

    /**
     * Simple constructor for unit testing.
     * @param readsFiles List of reads files to open.
     */
    public Reads( List<File> readsFiles ) {
        this.readsFiles = readsFiles;
        this.supplementalFilters = new ArrayList<SamRecordFilter>();
    }

    /**
     * Extract the command-line arguments having to do with reads input
     * files and store them in an easy-to-work-with package.  Constructor
     * is package protected.
     * @param samFiles list of reads files.
     * @param strictness Stringency of reads file parsing.
     * @param downsampleFraction fraction of reads to downsample.
     * @param downsampleCoverage downsampling per-locus.
     * @param beSafe Whether to enable safety checking.
     * @param supplementalFilters additional filters to dynamically apply.
     */
    Reads( List<File> samFiles,
           SAMFileReader.ValidationStringency strictness,
           Double downsampleFraction,
           Integer downsampleCoverage,
           Boolean beSafe,
           Collection<SamRecordFilter> supplementalFilters,
           int maximumReadsAtLocus,
           boolean includeReadsWithDeletionAtLoci) {
        this.readsFiles = samFiles;
        this.validationStringency = strictness;
        this.downsamplingFraction = downsampleFraction;
        this.downsampleToCoverage = downsampleCoverage;
        this.beSafe = beSafe;
        this.supplementalFilters = supplementalFilters;
        this.maximumReadsAtLocus = maximumReadsAtLocus;
        this.includeReadsWithDeletionAtLoci = includeReadsWithDeletionAtLoci;
    }
}
