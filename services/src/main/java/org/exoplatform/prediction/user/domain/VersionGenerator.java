package org.exoplatform.prediction.user.domain;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

public class VersionGenerator implements IdentifierGenerator  {

  @Override
  public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
    String name = ((ModelEntity) object).name;
    Connection connection = session.connection();
    try {

        PreparedStatement ps = connection
                .prepareStatement("SELECT MAX(m.version) as value FROM PredictionModel m WHERE m.name = ? GROUP BY m.name, m.status, m.created, m.datasetFile, m.modelFile, m.activated, m.archived");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Long value = rs.getLong("value");
            return value;
        }

    } catch (SQLException e) {       
        e.printStackTrace();
    }
    return null;
  }

}
