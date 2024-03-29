/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.clickhouse.cluster

import org.apache.spark.SparkConf
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

abstract class BaseClusterWriteSuite extends SparkClickHouseClusterTest {

  test("clickhouse write cluster") {
    withSimpleDistTable("single_replica", "db_w", "t_dist", true) { (_, db, tbl_dist, tbl_local) =>
      val tblSchema = spark.table(s"$db.$tbl_dist").schema
      assert(tblSchema == StructType(
        StructField("create_time", DataTypes.TimestampType, nullable = false) ::
          StructField("y", DataTypes.IntegerType, nullable = false) ::
          StructField("m", DataTypes.IntegerType, nullable = false) ::
          StructField("id", DataTypes.LongType, nullable = false) ::
          StructField("value", DataTypes.StringType, nullable = true) :: Nil
      ))

      checkAnswer(
        spark
          .table(s"$db.$tbl_dist")
          .select("create_time", "y", "m", "id", "value"),
        Seq(
          Row(timestamp("2021-01-01T10:10:10Z"), 2021, 1, 1L, "1"),
          Row(timestamp("2022-02-02T10:10:10Z"), 2022, 2, 2L, "2"),
          Row(timestamp("2023-03-03T10:10:10Z"), 2023, 3, 3L, "3"),
          Row(timestamp("2024-04-04T10:10:10Z"), 2024, 4, 4L, "4")
        )
      )

      checkAnswer(
        spark.table(s"clickhouse_s1r1.$db.$tbl_local"),
        Row(timestamp("2024-04-04T10:10:10Z"), 2024, 4, 4L, "4") :: Nil
      )
      checkAnswer(
        spark.table(s"clickhouse_s1r2.$db.$tbl_local"),
        Row(timestamp("2021-01-01T10:10:10Z"), 2021, 1, 1L, "1") :: Nil
      )
      checkAnswer(
        spark.table(s"clickhouse_s2r1.$db.$tbl_local"),
        Row(timestamp("2022-02-02T10:10:10Z"), 2022, 2, 2L, "2") :: Nil
      )
      checkAnswer(
        spark.table(s"clickhouse_s2r2.$db.$tbl_local"),
        Row(timestamp("2023-03-03T10:10:10Z"), 2023, 3, 3L, "3") :: Nil
      )
    }
  }
}

class ClusterNodesWriteSuite extends BaseClusterWriteSuite {

  override protected def sparkConf: SparkConf = super.sparkConf
    .set("spark.clickhouse.write.write.repartitionNum", "0")
    .set("spark.clickhouse.write.distributed.useClusterNodes", "true")
    .set("spark.clickhouse.write.distributed.convertLocal", "false")
}

class ConvertDistToLocalWriteSuite extends BaseClusterWriteSuite {

  override protected def sparkConf: SparkConf = super.sparkConf
    .set("spark.clickhouse.write.write.repartitionNum", "0")
    .set("spark.clickhouse.write.distributed.useClusterNodes", "true")
    .set("spark.clickhouse.write.distributed.convertLocal", "true")
}
