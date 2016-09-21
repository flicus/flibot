/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016 schors
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.schors.flibot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

public class VxZipInputStream implements ReadStream<Buffer> {

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;       // LOC header size
    static final int EXTHDR = 16;       // EXT header size
    static final int CENHDR = 46;       // CEN header size
    static final int ENDHDR = 22;       // END header size
    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;        // version needed to extract
    static final int LOCFLG = 6;        // general purpose bit flag
    static final int LOCHOW = 8;        // compression method
    static final int LOCTIM = 10;       // modification time
    static final int LOCCRC = 14;       // uncompressed file crc-32 value
    static final int LOCSIZ = 18;       // compressed size
    static final int LOCLEN = 22;       // uncompressed size
    static final int LOCNAM = 26;       // filename length
    static final int LOCEXT = 28;       // extra field length
    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;        // uncompressed file crc-32 value
    static final int EXTSIZ = 8;        // compressed size
    static final int EXTLEN = 12;       // uncompressed size
    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;        // version made by
    static final int CENVER = 6;        // version needed to extract
    static final int CENFLG = 8;        // encrypt, decrypt flags
    static final int CENHOW = 10;       // compression method
    static final int CENTIM = 12;       // modification time
    static final int CENCRC = 16;       // uncompressed file crc-32 value
    static final int CENSIZ = 20;       // compressed size
    static final int CENLEN = 24;       // uncompressed size
    static final int CENNAM = 28;       // filename length
    static final int CENEXT = 30;       // extra field length
    static final int CENCOM = 32;       // comment length
    static final int CENDSK = 34;       // disk number start
    static final int CENATT = 36;       // internal file attributes
    static final int CENATX = 38;       // external file attributes
    static final int CENOFF = 42;       // LOC header offset
    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;        // number of entries on this disk
    static final int ENDTOT = 10;       // total number of entries
    static final int ENDSIZ = 12;       // central directory size in bytes
    static final int ENDOFF = 16;       // offset of first CEN header
    static final int ENDCOM = 20;       // zip file comment length
    /*
     * Header signatures
     */
    static long LOCSIG = 0x04034b50L;   // "PK\003\004"
    static long EXTSIG = 0x08074b50L;   // "PK\007\008"
    static long CENSIG = 0x02014b50L;   // "PK\001\002"
    static long ENDSIG = 0x06054b50L;   // "PK\005\006"


    public VxZipInputStream(ReadStream<Buffer> input) {

    }

    public VxZipInputStream(ReadStream<Buffer> input, Inflater inflater) {

    }

    public void getNextEntry(Handler<AsyncResult<ZipEntry>> handler) {

    }

    @Override
    public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        return null;
    }

    @Override
    public ReadStream<Buffer> handler(Handler<Buffer> handler) {
        return null;
    }

    @Override
    public ReadStream<Buffer> pause() {
        return null;
    }

    @Override
    public ReadStream<Buffer> resume() {
        return null;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        return null;
    }
}
