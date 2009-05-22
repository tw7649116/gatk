package org.broadinstitute.sting.utils;

import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * User: aaron
 * Date: May 22, 2009
 * Time: 10:54:40 AM
 *
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT 
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 *
 */


/**
 * @author aaron
 * @version 1.0
 * @date May 22, 2009
 * <p/>
 * Class GenomeLocCollection
 * <p/>
 * a set of genome locations. This collection is self sorting,
 * and will merge genome locations that are overlapping. The remove function
 * will also remove a region from the list, if the region to remove is a
 * partial interval of a region in the collection it will remove the region from
 * that element.
 */
public class GenomeLocSet extends AbstractSet<GenomeLoc> {
    // our private storage for the GenomeLoc's
    private final ArrayList<GenomeLoc> mArray = new ArrayList<GenomeLoc>();

    public GenomeLocSet() {}

    /**
     * get an iterator over this collection
     *
     * @return
     */
    public Iterator<GenomeLoc> iterator() {
        return mArray.iterator();
    }

    /**
     * return the size of the collection
     *
     * @return
     */
    public int size() {
        return mArray.size();
    }

    /**
     * determine if the collection is empty
     *
     * @return true if we have no elements
     */
    public boolean isEmpty() {
        return mArray.isEmpty();
    }

    /**
     * add a genomeLoc to the collection, simply inserting in order into the set
     * @param e the GenomeLoc to add
     * @return true
     */
    public boolean add(GenomeLoc e) {
        if (mArray.contains(e)) {
            throw new IllegalArgumentException("attempting to add a duplicate object to the set");
        }
        int index = 0;
        while (index < mArray.size()) {
            if (!e.isPast(mArray.get(index))) {
                mArray.add(index,e);
                return true;
            }
            ++index;
        }
        this.mArray.add(e);
        return true;
    }

    /**
     * Adds a GenomeLoc to the collection, merging it if it overlaps another region.
     * If it's not overlapping then we add it in sorted order.
     *
     * @param e the GenomeLoc to add to the collection
     * @return true, if the GenomeLoc could be added to the collection
     */
    public boolean addRegion(GenomeLoc e) {
        if (e == null) {
            return false;
        }
        /**
         * check if the specified element overlaps any current locations, if so
         * we should merge the two.
         */
        for (GenomeLoc g : mArray) {
            if (g.contiguousP(e)) {
                GenomeLoc c = g.merge(e);
                mArray.set(mArray.indexOf(g),c);
                return true;
            } else if ((g.getContigIndex() == e.getContigIndex()) &&
                    (g.getStart() > e.getStart())) {
                mArray.add(mArray.indexOf(g), e);
                return true;
            }
        }
        /** we're at the end and we haven't found locations that should fall after it,
         * so we'll put it at the end
         */
        mArray.add(e);
        return true;
    }

    /**
     * remove an element from the set.  Given a specific genome location, this function will
     * remove all regions in the element set that overlap the specified region.
     * @param e the genomic range to remove
     * @return true if a removal action was performed, false if the collection was unchanged.
     */
    public boolean removeRegion(GenomeLoc e) {
        if (e == null) {
            return false;
        }

        // sometimes we can't return right away, this holds the value for those cases
        boolean returnValue = false;
        /**
         * check if the specified element overlaps any current locations, subtract the removed
         * region and reinsert what is left.
         */
        for (GenomeLoc g : mArray) {
            if (g.overlapsP(e)) {
                if (g.compareTo(e) == 0) {
                    mArray.remove(mArray.indexOf(g));
                    return true;
                } else if (g.containsP(e)) {
                    /**
                     * we have to create two new region, one for the before part, one for the after
                     * The old region:
                     * |----------------- old region (g) -------------|
                     *        |----- to delete (e) ------|
                     *
                     * product (two new regions):
                     * |------|  + |--------|
                     *
                     */
                    GenomeLoc before = new GenomeLoc(g.getContigIndex(), g.getStart(), e.getStart()-1);
                    GenomeLoc after = new GenomeLoc(g.getContigIndex(), e.getStop() + 1, g.getStop());
                    int index = mArray.indexOf(g);
                    mArray.add(index, after);
                    mArray.add(index, before);
                    mArray.remove(mArray.indexOf(g));
                    return true;
                } else if (e.containsP(g)) {
                    /**
                     * e completely contains g, delete g, but keep looking, there may be more regions
                     * i.e.:
                     *   |--------------------- e --------------------|
                     *       |--- g ---|    |---- others ----|
                     */
                    mArray.remove(mArray.indexOf(g));
                    returnValue = true;
                } else {

                    /**
                     * otherwise e overlaps some part of g
                     */
                    GenomeLoc l;

                    /**
                     * figure out which region occurs first on the genome.  I.e., is it:
                     * |------------- g ----------|
                     *       |------------- e ----------|
                     *
                     * or:
                     *       |------------- g ----------|
                     * |------------ e -----------|
                     *
                      */

                    if (e.getStart() < g.getStart()) {
                        l = new GenomeLoc(g.getContigIndex(), e.getStop()+1, g.getStop());
                    } else {
                        l = new GenomeLoc(g.getContigIndex(), g.getStart(), e.getStart()-1);
                    }
                    // replace g with the new region
                    mArray.set(mArray.indexOf(g), l);
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    /**
     * create a list of genomic locations, given a reference sequence
     * @param dict the sequence dictionary to create a collection from
     * @return the GenomeLocSet of all references sequences as GenomeLoc's
     */
    public static GenomeLocSet createSetFromSequenceDictionary(SAMSequenceDictionary dict) {
        GenomeLocSet returnSet = new GenomeLocSet();
        for (SAMSequenceRecord record : dict.getSequences()) {
            returnSet.add(new GenomeLoc(record.getSequenceIndex(),1,record.getSequenceLength()));
        }
        return returnSet;
    }
}
