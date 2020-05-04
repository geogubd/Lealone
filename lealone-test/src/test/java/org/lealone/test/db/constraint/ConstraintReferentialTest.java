/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.test.db.constraint;

import org.junit.Test;
import org.lealone.db.api.ErrorCode;

public class ConstraintReferentialTest extends ConstraintTestBase {
    @Test
    public void run() {
        executeUpdate("DROP TABLE IF EXISTS parent_table");
        executeUpdate("DROP TABLE IF EXISTS child_table");
        executeUpdate("CREATE TABLE IF NOT EXISTS parent_table (f1 int, f2 int)");
        executeUpdate("CREATE TABLE IF NOT EXISTS child_table (f1 int default -10, f2 int)");

        sql = "ALTER TABLE child_table ADD CONSTRAINT IF NOT EXISTS c_ref1 FOREIGN KEY(f1) REFERENCES parent_table(f1) ON DELETE CASCADE";
        executeUpdate(sql);
        assertFound("child_table", "c_ref1");

        try {
            executeUpdate("insert into child_table(f1) values(10)"); // parent_table还没有记录
            fail();
        } catch (Exception e) {
            assertException(e, ErrorCode.REFERENTIAL_INTEGRITY_VIOLATED_PARENT_MISSING_1);
        }

        // 测试ON DELETE CASCADE

        executeUpdate("insert into parent_table(f1) values(10)");
        executeUpdate("insert into child_table(f1) values(10)");
        assertEquals(1, getInt("select count(*) from parent_table", 1));
        assertEquals(1, getInt("select count(*) from child_table", 1));
        executeUpdate("delete from parent_table where f1=10");
        assertEquals(0, getInt("select count(*) from parent_table", 1));
        assertEquals(0, getInt("select count(*) from child_table", 1));

        sql = "ALTER TABLE child_table DROP CONSTRAINT IF EXISTS c_ref1";
        executeUpdate(sql);
        assertNotFound("child_table", "c_ref1");

        // 测试ON DELETE SET DEFAULT

        sql = "ALTER TABLE child_table ADD CONSTRAINT IF NOT EXISTS c_ref2 FOREIGN KEY(f1) REFERENCES parent_table(f1)"
                + " ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL"
                + " ON DELETE SET DEFAULT NOT DEFERRABLE";
        sql = "ALTER TABLE child_table ADD CONSTRAINT IF NOT EXISTS c_ref2 FOREIGN KEY(f1) REFERENCES parent_table(f1)"
                + " ON DELETE SET DEFAULT";
        executeUpdate(sql);
        assertFound("child_table", "c_ref2");

        executeUpdate("insert into parent_table(f1) values(10)");
        executeUpdate("insert into child_table(f1) values(10)");
        assertEquals(1, getInt("select count(*) from parent_table", 1));
        assertEquals(1, getInt("select count(*) from child_table", 1));
        executeUpdate("delete from parent_table where f1=10");
        assertEquals(0, getInt("select count(*) from parent_table", 1));
        assertEquals(1, getInt("select count(*) from child_table", 1));
        assertEquals(-10, getInt("select f1 from child_table", 1));

        sql = "ALTER TABLE child_table DROP CONSTRAINT IF EXISTS c_ref2";
        executeUpdate(sql);
        assertNotFound("child_table", "c_ref2");
    }
}
