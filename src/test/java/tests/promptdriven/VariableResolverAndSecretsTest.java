package tests.promptdriven;

import framework.promptdriven.exec.SecretsProvider;
import framework.promptdriven.exec.VariableResolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class VariableResolverAndSecretsTest {

    @Test
    public void testVariableResolutionAndDotNotation() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("token", "xyz-12345");
        vars.put("user", Map.of("email", "john@example.com", "role", "ADMIN"));

        SecretsProvider secrets = new SecretsProvider();
        VariableResolver resolver = new VariableResolver(vars, secrets);

        Assert.assertEquals(resolver.resolve("Bearer ${token}"), "Bearer xyz-12345");
        Assert.assertEquals(resolver.resolve("Email is ${user.email}"), "Email is john@example.com");
        Assert.assertEquals(resolver.resolve("Role is ${user.role}"), "Role is ADMIN");
        Assert.assertEquals(resolver.resolve("Stored is ${stored.token}"), "Stored is xyz-12345");
    }

    @Test
    public void testSecretMaskingAndLeakageGrep() {
        SecretsProvider secrets = new SecretsProvider();
        String superSecretPassword = "UltraConfidentialP@ss999!";
        secrets.registerSecretValue(superSecretPassword);

        Map<String, Object> vars = new HashMap<>();
        VariableResolver resolver = new VariableResolver(vars, secrets);

        String sampleLog = "User admin logged in with password " + superSecretPassword + " at timestamp 123456";
        String masked = resolver.mask(sampleLog);

        Assert.assertFalse(masked.contains(superSecretPassword), "Secret leaked in masked string!");
        Assert.assertTrue(masked.contains("********"), "Mask token missing in masked log output!");

        // Assert secret does not appear anywhere in masked output
        Assert.assertEquals(masked, "User admin logged in with password ******** at timestamp 123456");
    }
}
