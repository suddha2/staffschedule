package staffschedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.lang.Keys;

class KeyGen {

	@Test
	void test() {

		KeyPair keyPair = Keys.keyPairFor(Version.V2); // Generates Ed25519 keys for v2.public
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		try {
			Files.write(Paths.get("private.key"), Base64.getEncoder().encode(privateKey.getEncoded()));
			Files.write(Paths.get("public.key"), Base64.getEncoder().encode(publicKey.getEncoded()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
