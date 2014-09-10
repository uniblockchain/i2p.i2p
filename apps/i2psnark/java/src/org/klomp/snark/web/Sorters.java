package org.klomp.snark.web;

import java.io.Serializable;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.klomp.snark.Snark;

/**
 *  Comparators for various columns
 *
 *  @since 0.9.16 moved from I2PSnarkservlet
 */
class Sorters {

    public static Comparator<Snark> getComparator(int type) {
        boolean rev = type < 0;
        Comparator<Snark> rv;
        switch (type) {

          case -1:
          case 0:
          case 1:
          default:
              rv = new TorrentNameComparator();
              if (rev)
                  rv = Collections.reverseOrder(rv);
              break;

          case -2:
          case 2:
              rv = new StatusComparator(rev);
              break;

          case -3:
          case 3:
              rv = new PeersComparator(rev);
              break;

          case -4:
          case 4:
              rv = new ETAComparator(rev);
              break;

          case -5:
          case 5:
              rv = new SizeComparator(rev);
              break;

          case -6:
          case 6:
              rv = new DownloadedComparator(rev);
              break;

          case -7:
          case 7:
              rv = new UploadedComparator(rev);
              break;

          case -8:
          case 8:
              rv = new DownRateComparator(rev);
              break;

          case -9:
          case 9:
              rv = new UpRateComparator(rev);
              break;

          case -10:
          case 10:
              rv = new RemainingComparator(rev);
              break;

        }
        return rv;
    }


    /**
     *  Sort alphabetically in current locale, ignore case, ignore leading "the "
     *  (I guess this is worth it, a lot of torrents start with "The "
     *  @since 0.7.14
     */
    private static class TorrentNameComparator implements Comparator<Snark>, Serializable {

        public int compare(Snark l, Snark r) {
            return comp(l, r);
        }

        public static int comp(Snark l, Snark r) {
            // put downloads and magnets first
            if (l.getStorage() == null && r.getStorage() != null)
                return -1;
            if (l.getStorage() != null && r.getStorage() == null)
                return 1;
            String ls = l.getBaseName();
            String llc = ls.toLowerCase(Locale.US);
            if (llc.startsWith("the ") || llc.startsWith("the.") || llc.startsWith("the_"))
                ls = ls.substring(4);
            String rs = r.getBaseName();
            String rlc = rs.toLowerCase(Locale.US);
            if (rlc.startsWith("the ") || rlc.startsWith("the.") || rlc.startsWith("the_"))
                rs = rs.substring(4);
            return Collator.getInstance().compare(ls, rs);
        }
    }

    /**
     *  Forward or reverse sort, but the fallback is always forward
     */
    private static abstract class Sort implements Comparator<Snark>, Serializable {

        private final boolean _rev;

        public Sort(boolean rev) {
            _rev = rev;
        }

        public int compare(Snark l, Snark r) {
            int rv = compareIt(l, r);
            if (rv != 0)
                return _rev ? 0 - rv : rv;
            return TorrentNameComparator.comp(l, r);
        }

        protected abstract int compareIt(Snark l, Snark r);

        protected static int compLong(long l, long r) {
            if (l < r)
                return -1;
            if (l > r)
                return 1;
            return 0;
        }
    }


    private static class StatusComparator extends Sort {

        private StatusComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return getStatus(l) - getStatus(r);
        }

        private static int getStatus(Snark snark) {
            long remaining = snark.getRemainingLength(); 
            long needed = snark.getNeededLength(); 
            if (snark.isStopped()) {
                if (remaining < 0)
                    return 0;
                if (remaining > 0)
                    return 5;
                return 10;
            }
            if (snark.isStarting())
                return 15;
            if (snark.isAllocating())
                return 20;
            if (remaining < 0)
                return 15; // magnet
            if (remaining == 0)
                return 100;
            if (snark.isChecking())
                return 95;
            if (snark.getNeededLength() <= 0)
                return 90;
            if (snark.getPeerCount() <= 0)
                return 40;
            if (snark.getDownloadRate() <= 0)
                return 50;
            return 60;
        }
    }

    private static class PeersComparator extends Sort {

        public PeersComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return l.getPeerCount() - r.getPeerCount();
        }
    }

    private static class RemainingComparator extends Sort {

        public RemainingComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getNeededLength(), r.getNeededLength());
        }
    }

    private static class ETAComparator extends Sort {

        public ETAComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(eta(l), eta(r));
        }

        private static long eta(Snark snark) {
            long needed = snark.getNeededLength(); 
            long total = snark.getTotalLength();
            if (needed > total)
                needed = total;
            long remainingSeconds;
            long downBps = snark.getDownloadRate();
            if (downBps > 0 && needed > 0)
                return needed / downBps;
            return -1;
        }
    }

    private static class SizeComparator extends Sort {

        public SizeComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getTotalLength(), r.getTotalLength());
        }
    }
    private static class DownloadedComparator extends Sort {

        public DownloadedComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getDownloaded(), r.getDownloaded());
        }
    }

    private static class UploadedComparator extends Sort {

        public UploadedComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getUploaded(), r.getUploaded());
        }
    }

    private static class DownRateComparator extends Sort {

        public DownRateComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getDownloadRate(), r.getDownloadRate());
        }
    }

    private static class UpRateComparator extends Sort {

        public UpRateComparator(boolean rev) { super(rev); }

        public int compareIt(Snark l, Snark r) {
            return compLong(l.getUploadRate(), r.getUploadRate());
        }
    }
}
