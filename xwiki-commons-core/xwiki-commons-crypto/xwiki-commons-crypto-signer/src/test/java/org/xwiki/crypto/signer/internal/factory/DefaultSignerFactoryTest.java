/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.crypto.signer.internal.factory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA224DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA256DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA384DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA512DigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link DefaultSignerFactory}.
 *
 * @version $Id$
 */
@ComponentList({Base64BinaryStringEncoder.class, BcRSAKeyFactory.class, BcSHA1DigestFactory.class,
    BcSHA224DigestFactory.class, BcSHA256DigestFactory.class, BcSHA384DigestFactory.class,
    BcSHA512DigestFactory.class, BcSHA1withRsaSignerFactory.class, BcSHA224withRsaSignerFactory.class,
    BcSHA256withRsaSignerFactory.class, BcSHA384withRsaSignerFactory.class, BcSHA512withRsaSignerFactory.class,
    BcRsaSsaPssSignerFactory.class, BcMD5withRsaSignerFactory.class})
public class DefaultSignerFactoryTest extends AbstractRsaSignerFactoryTest
{
    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mocker =
        new MockitoComponentMockingRule<>(DefaultSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerPss =
        new MockitoComponentMockingRule<>(BcRsaSsaPssSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerSha1 =
        new MockitoComponentMockingRule<>(BcSHA1withRsaSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerSha224 =
        new MockitoComponentMockingRule<>(BcSHA224withRsaSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerSha256 =
        new MockitoComponentMockingRule<>(BcSHA256withRsaSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerSha384 =
        new MockitoComponentMockingRule<>(BcSHA384withRsaSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerSha512 =
        new MockitoComponentMockingRule<>(BcSHA512withRsaSignerFactory.class);

    @Rule
    public final MockitoComponentMockingRule<SignerFactory> mockerMD5 =
        new MockitoComponentMockingRule<>(BcMD5withRsaSignerFactory.class);

    private SignerFactory factory;

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
        setupTest(mocker);
    }

    private void runTest(MockitoComponentMockingRule<SignerFactory> mocker) throws Exception
    {
        Signer signer = mocker.getComponentUnderTest().getInstance(true, privateKey);
        runTestSignatureVerification(
            signer,
            factory.getInstance(false, publicKey, signer.getEncoded())
        );
    }

    @Test
    public void testPssSignatureVerification() throws Exception
    {
        runTest(mockerPss);
    }

    @Test
    public void testSha1SignatureVerification() throws Exception
    {
        runTest(mockerSha1);
    }

    @Test
    public void testSha224SignatureVerification() throws Exception
    {
        runTest(mockerSha224);
    }

    @Test
    public void testSha256SignatureVerification() throws Exception
    {
        runTest(mockerSha256);
    }

    @Test
    public void testSha384SignatureVerification() throws Exception
    {
        runTest(mockerSha384);
    }

    @Test
    public void testSha512SignatureVerification() throws Exception
    {
        runTest(mockerSha512);
    }

    @Test
    public void testMd5SignatureVerification() throws Exception
    {
        runTest(mockerMD5);
    }
}
