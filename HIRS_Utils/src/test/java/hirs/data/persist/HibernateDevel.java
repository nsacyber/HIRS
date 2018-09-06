package hirs.data.persist;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateDevel {

	public static void main(String[] args) throws Exception {
		
		Configuration c = new Configuration();
		c.configure();
		Properties settings = c.getProperties();
		
//        c.generateSchemaCreationScript(Dialect.getDialect(settings));
//        SchemaExport export = new SchemaExport(c);
//        export.setDelimiter(";");
//        export.setOutputFile("_create_schema.sql");
//        export.setFormat(true);
//        export.execute(true, true, false, true);

		
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySettings(settings);
		StandardServiceRegistry registry = ssrb.build();
		
		SessionFactory sessionFactory = c.buildSessionFactory(registry);
		
		Session session = sessionFactory.getCurrentSession();
		
		session.close();
		sessionFactory.close();
	}
}
