package com.example.vericert.service;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cms.CMSTypedData;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class CMSProcessableInputStream implements CMSTypedData {
    private final byte[] data;

    public CMSProcessableInputStream(byte[] is) {
        this.data = Objects.requireNonNull(is);
    }

    @Override
    public ASN1ObjectIdentifier getContentType() {
        return CMSObjectIdentifiers.data;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        out.write(data);
    }

    @Override
    public Object getContent() {
        return data;
    }
}
