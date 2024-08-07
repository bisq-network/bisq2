/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.data;



/*
 * Copyright 2011 Fabian Barney
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Fabian Barney
 */
// Taken from https://raw.githubusercontent.com/fabian-barney/Utils/master/utils/src/com/barney4j/utils/unit/ByteUnit.java

//todo would need a bit adjustment. not sure if we keep it...
@SuppressWarnings("SpellCheckingInspection")
public enum ByteUnit {

    /**
     * <pre>
     * Byte (B)
     * 1 Byte
     */
    BYTE {
        @Override
        public double toBytes(double d) {
            return d;
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toBytes(d);
        }
    },

    /**
     * <pre>
     * Kibibyte (KiB)
     * 2^10 Byte = 1.024 Byte
     */
    KIB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_KIB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toKiB(d);
        }
    },

    /**
     * <pre>
     * Mebibyte (MiB)
     * 2^20 Byte = 1.024 * 1.024 Byte = 1.048.576 Byte
     */
    MIB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_MIB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toMiB(d);
        }
    },

    /**
     * <pre>
     * Gibibyte (GiB)
     * 2^30 Byte = 1.024 * 1.024 * 1.024 Byte = 1.073.741.824 Byte
     */
    GIB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_GIB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toGiB(d);
        }
    },

    /**
     * <pre>
     * Tebibyte (TiB)
     * 2^40 Byte = 1.024 * 1.024 * 1.024 * 1.024 Byte = 1.099.511.627.776 Byte
     */
    TIB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_TIB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toTiB(d);
        }
    },

    /**
     * <pre>
     * Pebibyte (PiB)
     * 2^50 Byte = 1.024 * 1.024 * 1.024 * 1.024 * 1.024 Byte = 1.125.899.906.842.624 Byte
     */
    PIB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_PIB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toPiB(d);
        }
    },

    /**
     * <pre>
     * Kilobyte (kB)
     * 10^3 Byte = 1.000 Byte
     */
    KB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_KB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toKB(d);
        }
    },

    /**
     * <pre>
     * Megabyte (MB)
     * 10^6 Byte = 1.000.000 Byte
     */
    MB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_MB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toMB(d);
        }
    },

    /**
     * <pre>
     * Gigabyte (GB)
     * 10^9 Byte = 1.000.000.000 Byte
     */
    GB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_GB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toGB(d);
        }
    },

    /**
     * <pre>
     * Terabyte (TB)
     * 10^12 Byte = 1.000.000.000.000 Byte
     */
    TB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_TB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toTB(d);
        }
    },

    /**
     * <pre>
     * Petabyte (PB)
     * 10^15 Byte = 1.000.000.000.000.000 Byte
     */
    PB {
        @Override
        public double toBytes(double d) {
            return safeMulti(d, C_PB);
        }

        @Override
        public double convert(double d, ByteUnit u) {
            return u.toPB(d);
        }
    };


    static final double C_KIB = Math.pow(2d, 10d);
    static final double C_MIB = Math.pow(2d, 20d);
    static final double C_GIB = Math.pow(2d, 30d);
    static final double C_TIB = Math.pow(2d, 40d);
    static final double C_PIB = Math.pow(2d, 50d);

    static final double C_KB = Math.pow(10d, 3d);
    static final double C_MB = Math.pow(10d, 6d);
    static final double C_GB = Math.pow(10d, 9d);
    static final double C_TB = Math.pow(10d, 12d);
    static final double C_PB = Math.pow(10d, 15d);


    private static final double MAX = Double.MAX_VALUE;


    static double safeMulti(double d, double multi) {
        double limit = MAX / multi;

        if (d > limit) {
            return Double.MAX_VALUE;
        }
        if (d < -limit) {
            return Double.MIN_VALUE;
        }

        return d * multi;
    }


    public abstract double toBytes(double d);

    public final double toKiB(double d) {
        return toBytes(d) / C_KIB;
    }

    public final double toMiB(double d) {
        return toBytes(d) / C_MIB;
    }

    public final double toGiB(double d) {
        return toBytes(d) / C_GIB;
    }

    public final double toTiB(double d) {
        return toBytes(d) / C_TIB;
    }

    public final double toPiB(double d) {
        return toBytes(d) / C_PIB;
    }


    public final double toKB(double d) {
        return toBytes(d) / C_KB;
    }

    public final double toMB(double d) {
        return toBytes(d) / C_MB;
    }

    public final double toGB(double d) {
        return toBytes(d) / C_GB;
    }

    public final double toTB(double d) {
        return toBytes(d) / C_TB;
    }

    public final double toPB(double d) {
        return toBytes(d) / C_PB;
    }


    public abstract double convert(double d, ByteUnit u);

    public final double convert(double d, BitUnit u) {
        return convert(d, u, Byte.SIZE);
    }

    public final double convert(double d, BitUnit u, int wordSize) {
        double bytes = u.toBits(d) / wordSize;
        return convert(bytes, BYTE);
    }


    /*
     * Komfort-Methoden f�r Cross-Konvertierung
     */
    public final double toBits(double d) {
        return BitUnit.BIT.convert(d, this);
    }

    public final double toBits(double d, int wordSize) {
        return BitUnit.BIT.convert(d, this, wordSize);
    }


    public final double toKibit(double d) {
        return BitUnit.KIBIT.convert(d, this);
    }

    public final double toMibit(double d) {
        return BitUnit.MIBIT.convert(d, this);
    }

    public final double toGibit(double d) {
        return BitUnit.GIBIT.convert(d, this);
    }

    public final double toTibit(double d) {
        return BitUnit.TIBIT.convert(d, this);
    }

    public final double toPibit(double d) {
        return BitUnit.PIBIT.convert(d, this);
    }

    public final double toKibit(double d, int wordSize) {
        return BitUnit.KIBIT.convert(d, this, wordSize);
    }

    public final double toMibit(double d, int wordSize) {
        return BitUnit.MIBIT.convert(d, this, wordSize);
    }

    public final double toGibit(double d, int wordSize) {
        return BitUnit.GIBIT.convert(d, this, wordSize);
    }

    public final double toTibit(double d, int wordSize) {
        return BitUnit.TIBIT.convert(d, this, wordSize);
    }

    public final double toPibit(double d, int wordSize) {
        return BitUnit.PIBIT.convert(d, this, wordSize);
    }


    public final double toKbit(double d) {
        return BitUnit.KBIT.convert(d, this);
    }

    public final double toMbit(double d) {
        return BitUnit.MBIT.convert(d, this);
    }

    public final double toGbit(double d) {
        return BitUnit.GBIT.convert(d, this);
    }

    public final double toTbit(double d) {
        return BitUnit.TBIT.convert(d, this);
    }

    public final double toPbit(double d) {
        return BitUnit.PBIT.convert(d, this);
    }


    public final double toKbit(double d, int wordSize) {
        return BitUnit.KBIT.convert(d, this, wordSize);
    }

    public final double toMbit(double d, int wordSize) {
        return BitUnit.MBIT.convert(d, this, wordSize);
    }

    public final double toGbit(double d, int wordSize) {
        return BitUnit.GBIT.convert(d, this, wordSize);
    }

    public final double toTbit(double d, int wordSize) {
        return BitUnit.TBIT.convert(d, this, wordSize);
    }

    public final double toPbit(double d, int wordSize) {
        return BitUnit.PBIT.convert(d, this, wordSize);
    }

}