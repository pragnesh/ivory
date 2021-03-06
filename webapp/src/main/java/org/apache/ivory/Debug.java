package org.apache.ivory;

import org.apache.ivory.client.IvoryClient;
import org.apache.ivory.entity.EntityUtil;
import org.apache.ivory.entity.store.ConfigurationStore;
import org.apache.ivory.entity.v0.Frequency;
import org.apache.ivory.entity.v0.process.Process;
import org.apache.ivory.entity.v0.Entity;
import org.apache.ivory.entity.v0.EntityType;
import org.apache.ivory.entity.v0.feed.Feed;
import org.apache.ivory.security.CurrentUser;
import org.apache.ivory.service.Services;
import org.apache.ivory.util.DeploymentProperties;
import org.apache.ivory.util.StartupProperties;
import org.apache.ivory.workflow.engine.OozieWorkflowEngine;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;

public class Debug {
    private static final Logger LOG = Logger.getLogger(Debug.class);

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

    public static void main(String[] args) throws Exception {
        String ivoryUrl = args[0];
        String type = args[1];
        String entity;

        Services.get().register(ConfigurationStore.get());
        ConfigurationStore.get().init();
        CurrentUser.authenticate("testuser");
        IvoryClient client = new IvoryClient(ivoryUrl);
        for (int index = 2; index < args.length; index++) {
            entity = args[index];
            String[] deps = client.getDependency(type, entity).split("\n");
            for (String line : deps) {
                String[] fields = line.replace("(", "").replace(")", "").split(" ");
                EntityType eType = EntityType.valueOf(fields[0].toUpperCase());
                if (ConfigurationStore.get().get(eType, fields[1]) != null) continue;
                String xml = client.getDefinition(eType.name().toLowerCase(), fields[1]);
                System.out.println(xml);
                store(eType, xml);
            }
            String xml = client.getDefinition(type.toLowerCase(), entity);
            System.out.println(xml);
            store(EntityType.valueOf(type.toUpperCase()), xml);
        }

        entity = args[2];
        Entity obj = EntityUtil.getEntity(type, entity);
        Process newEntity = (Process)obj.clone();
        newEntity.setFrequency(Frequency.fromString("minutes(5)"));
        System.out.println("##############OLD ENTITY " + EntityUtil.md5(obj));
        System.out.println("##############NEW ENTITY " + EntityUtil.md5(newEntity));


//        OozieWorkflowEngine engine = new OozieWorkflowEngine();
//        Date start = formatter.parse("2010-01-02 01:05 UTC");
//        Date end = formatter.parse("2010-01-02 01:21 UTC");
//        InstancesResult status = engine.suspendInstances(obj, start, end, new Properties());
//        System.out.println(Arrays.toString(status.getInstances()));
//        AbstractInstanceManager manager = new InstanceManager();
//        InstancesResult result = manager.suspendInstance(new NullServletRequest(), type, entity,
//                "2010-01-02T01:05Z", "2010-01-02T01:21Z", "*");

        DeploymentProperties.get().setProperty("deploy.mode", "standalone");
        StartupProperties.get().setProperty("current.colo", "ua1");
        OozieWorkflowEngine engine = new OozieWorkflowEngine();
        ConfigurationStore.get().initiateUpdate(newEntity);
        engine.update(obj, newEntity, newEntity.getClusters().getClusters().get(0).getName());
        engine.delete(newEntity);
        System.exit(0);
    }

    private static void store(EntityType eType, String xml) throws JAXBException, IvoryException {
        Unmarshaller unmarshaller = eType.getUnmarshaller();
        Entity obj = (Entity) unmarshaller.unmarshal(new
                ByteArrayInputStream(xml.getBytes()));
        ConfigurationStore.get().publish(eType, obj);
    }
}
