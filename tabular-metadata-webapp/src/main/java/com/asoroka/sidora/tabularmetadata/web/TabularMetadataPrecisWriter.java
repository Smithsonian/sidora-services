/**
 * 
 */

package com.asoroka.sidora.tabularmetadata.web;

import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

/**
 * @author ajs6f
 */
@Provider
public class TabularMetadataPrecisWriter implements MessageBodyWriter<TabularMetadataPrecis> {

    private static final QName XML_NAME = new QName("tabular-metadata");

    private static final JAXBContext context;

    static {
        try {
            context = newInstance(TabularMetadataPrecis.class);
        } catch (final JAXBException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void writeTo(final TabularMetadataPrecis t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
            throws WebApplicationException {

        try {
            final Marshaller m = context.createMarshaller();
            m.setProperty(JAXB_FORMATTED_OUTPUT, true);
            final JAXBElement<TabularMetadataPrecis> jaxb =
                    new JAXBElement<>(XML_NAME, TabularMetadataPrecis.class, t);
            m.marshal(jaxb, entityStream);
        } catch (final JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return type.isAssignableFrom(TabularMetadataPrecis.class);
    }

    @Override
    public long getSize(final TabularMetadataPrecis t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }
}
