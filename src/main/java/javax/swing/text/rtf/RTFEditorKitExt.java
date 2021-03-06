/*
 * Copyright 1997-2000 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package javax.swing.text.rtf;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * This is the default implementation of RTF editing
 * functionality.  The RTF support was not written by the
 * Swing team.  In the future we hope to improve the support
 * provided.
 *
 * @author Timothy Prinzing (of this class, not the package!)
 */
public class RTFEditorKitExt extends StyledEditorKit {

	/**
	 * NOTE: Default UID generated, change if necessary.
	 */
	private static final long serialVersionUID = 1268406427851826229L;

	/**
	 * Constructs an RTFEditorKit.
	 */
	public RTFEditorKitExt() {
		super();
	}

	/**
	 * Get the MIME type of the data that this
	 * kit represents support for.  This kit supports
	 * the type <code>text/rtf</code>.
	 *
	 * @return the type
	 */
	public String getContentType() {
		return "text/rtf";
	}

	/**
	 * Insert content from the given stream which is expected
	 * to be in a format appropriate for this kind of content
	 * handler.
	 *
	 * @param in  The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the
	 *            content.
	 * @throws IOException          on any I/O error
	 * @throws BadLocationException if pos represents an invalid
	 *                              location within the document.
	 */
	public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException {

		if (doc instanceof StyledDocument) {
			// PENDING(prinz) this needs to be fixed to
			// insert to the given position.
			RTFReaderExt rdr = new RTFReaderExt((StyledDocument) doc);
			rdr.readFromStream(in);
			rdr.close();
		} else {
			// treat as text/plain
			super.read(in, doc, pos);
		}
	}

	/**
	 * Write content from a document to the given stream
	 * in a format appropriate for this kind of content handler.
	 *
	 * @param out The stream to write to
	 * @param doc The source for the write.
	 * @param pos The location in the document to fetch the
	 *            content.
	 * @param len The amount to write out.
	 * @throws IOException          on any I/O error
	 * @throws BadLocationException if pos represents an invalid
	 *                              location within the document.
	 */
	public void write(OutputStream out, Document doc, int pos, int len)
			throws IOException, BadLocationException {

		// PENDING(prinz) this needs to be fixed to
		// use the given document range.
		RTFGeneratorExt.writeDocument(doc, out);
	}

	/**
	 * Insert content from the given stream, which will be
	 * treated as plain text.
	 *
	 * @param in  The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the
	 *            content.
	 * @throws IOException          on any I/O error
	 * @throws BadLocationException if pos represents an invalid
	 *                              location within the document.
	 */
	public void read(Reader in, Document doc, int pos)
			throws IOException, BadLocationException {

		if (doc instanceof StyledDocument) {
			RTFReaderExt rdr = new RTFReaderExt((StyledDocument) doc);
			rdr.readFromReader(in);
			rdr.close();
		} else {
			// treat as text/plain
			super.read(in, doc, pos);
		}
	}

	/**
	 * Write content from a document to the given stream
	 * as plain text.
	 *
	 * @param out The stream to write to
	 * @param doc The source for the write.
	 * @param pos The location in the document to fetch the
	 *            content.
	 * @param len The amount to write out.
	 * @throws IOException          on any I/O error
	 * @throws BadLocationException if pos represents an invalid
	 *                              location within the document.
	 */
	public void write(Writer out, Document doc, int pos, int len)
			throws IOException, BadLocationException {

		throw new IOException("RTF is an 8-bit format");
	}

}
