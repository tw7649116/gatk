/*
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.contexts;

import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMRecord;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.StingException;
import org.broadinstitute.sting.utils.pileup.ReadBackedPileup;

import java.util.*;

/**
 * Useful class for forwarding on locusContext data from this iterator
 * 
 * Created by IntelliJ IDEA.
 * User: mdepristo
 * Date: Feb 22, 2009
 * Time: 3:01:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlignmentContext {
    protected GenomeLoc loc = null;
    protected ReadBackedPileup pileup = null;

    /**
     * The number of bases we've skipped over in the reference since the last map invocation.
     * Only filled in by RodTraversals right now.  By default, nothing is being skipped, so skippedBases == 0.
     */
    private long skippedBases = 0;

    /**
     * Default constructor for AlignmentContext object
     * since private objects are already set to null we
     * don't need to do anything
     */
    public AlignmentContext() { /*   private objects already set to null    */  }

    /**
     * Create a new AlignmentContext object
     *
     * @param loc
     * @param reads
     * @param offsets
     */
    @Deprecated
    public AlignmentContext(GenomeLoc loc, List<SAMRecord> reads, List<Integer> offsets) {
        this(loc, reads, offsets, 0);
    }

    @Deprecated
    public AlignmentContext(GenomeLoc loc, List<SAMRecord> reads, List<Integer> offsets, long skippedBases ) {
        if ( loc == null ) throw new StingException("BUG: GenomeLoc in Alignment context is null");
        if ( skippedBases < 0 ) throw new StingException("BUG: skippedBases is -1 in Alignment context");

        this.loc = loc;
        this.pileup = new ReadBackedPileup(loc, reads, offsets);
        this.skippedBases = skippedBases;
    }

    public AlignmentContext(GenomeLoc loc, ReadBackedPileup pileup ) {
        this(loc, pileup, 0);
    }


    public AlignmentContext(GenomeLoc loc, ReadBackedPileup pileup, long skippedBases ) {
        if ( loc == null ) throw new StingException("BUG: GenomeLoc in Alignment context is null");
        if ( pileup == null ) throw new StingException("BUG: ReadBackedPileup in Alignment context is null");
        if ( skippedBases < 0 ) throw new StingException("BUG: skippedBases is -1 in Alignment context");

        this.loc = loc;
        this.pileup = pileup;
        this.skippedBases = skippedBases;
    }

    public ReadBackedPileup getPileup() { return pileup; }

    /**
     * get all of the reads within this context
     * 
     * @return
     */
    @Deprecated
    public List<SAMRecord> getReads() { return pileup.getReads(); }

    /**
     * Are there any reads associated with this locus?
     *
     * @return
     */
    public boolean hasReads() {
        return pileup.size() > 0;
    }

    /**
     * How many reads cover this locus?
     * @return
     */
    public int size() {
        return pileup.size();
    }

    /**
     * get a list of the equivalent positions within in the reads at Pos
     *
     * @return
     */
    @Deprecated
    public List<Integer> getOffsets() {
        return pileup.getOffsets();
    }

    public String getContig() { return getLocation().getContig(); }
    public long getPosition() { return getLocation().getStart(); }
    public GenomeLoc getLocation() { return loc; }

    public void downsampleToCoverage(int coverage) {
        pileup = pileup.getDownsampledPileup(coverage);
    }

    /**
     * Returns the number of bases we've skipped over in the reference since the last map invocation.
     * Only filled in by RodTraversals right now.  A value of 0 indicates that no bases were skipped.
     *
     * @return the number of skipped bases
     */
    public long getSkippedBases() {
        return skippedBases;
    }
}
