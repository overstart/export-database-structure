package com.msw.java;

import com.alibaba.fastjson.JSON;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.DocxRenderData;
import com.deepoove.poi.data.MiniTableRenderData;
import com.deepoove.poi.data.RowRenderData;
import com.deepoove.poi.data.TextRenderData;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把数据库中的表结构导出word中
 *
 * @author MOSHUNWEI
 * @version 1.0
 */
public class App {
    public static void main(String[] args) throws IOException {
        //使用jar的命令，参数有4个，如果没有4个直接退出，没得商量
        if (args.length < 6) {
            System.out.println("参数：");
            System.out.println("-n=数据库名称");
            System.out.println("-u=用户名");
            System.out.println("-p=密码");
            System.out.println("-d=文件输出路径");
            System.out.println("h=数据库地址");
            System.out.println("p=数据库端口");
            System.out.println("-t=mysql or oracle defalut mysql");
            System.exit(0);
        }

        Map<String, String> map = Check(args);

        if (map.get("-t") == null || map.get("-t").equals("mysql")) {
            MySQL(map);
        } else if (map.get("-t").equals("oracle")) {
            Oracle(map);
        } else {
            System.out.println("-t=mysql or oracle defalut mysql");
            System.exit(0);
        }

    }

    public static void Oracle(Map<String, String> map) throws IOException {
        //默认生成的文件名
        String outFile = map.get("-d") + "/数据库表结构(ORACLE).docx";
        //查询表的名称以及一些表需要的信息
        String oracleSql1 = "select ut.table_name as table_name,ut.tablespace_name as engine,ut.buffer_pool as table_collation, uc.table_type as table_type,uc.comments as table_comment,ut.last_analyzed as create_options from user_tables ut,user_tab_comments uc where ut.table_name=uc.table_name";
        String oracleSql2 = "select rownum as ordinal_position,c.nullable as is_nullable,c.data_default as column_default,c.data_type as data_type,c.data_length as character_maximum_length,t.column_name as column_name,t.comments as column_comment from user_col_comments t,user_tab_columns c where c.column_name=t.column_name and c.table_name=t.table_name and t.table_name='";
        ResultSet rs = OracleUtils.getResultSet(OracleUtils.getConnnection(String.format("jdbc:oracle:thin:@%s:%s:ORCL", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), oracleSql1);
        Connection con = OracleUtils.getConnnection(String.format("jdbc:oracle:thin:@%s:%s:ORCL", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        createDoc(rs, oracleSql2, "", map, outFile, false, "Oracle数据库表结构", con);
    }

    public static void MySQL(Map<String, String> map) throws IOException {
        //默认生成的文件名
        String outFile = map.get("-d") + "/" + map.get("-n") + "数据库表结构(MySQL).docx";
        //查询表的名称以及一些表需要的信息
        String mysqlSql1 = "SELECT table_name, table_type , ENGINE,table_collation,table_comment, create_options FROM information_schema.TABLES WHERE table_schema='" + map.get("-n") + "' ORDER BY TABLE_NAME ASC";
        //查询表的结构信息
        String mysqlSql2 = "SELECT ordinal_position,column_name,column_type, column_key, extra ,is_nullable, column_default, column_comment,data_type,character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema='" + map.get("-n") + "' and table_name='";
        ResultSet rs = SqlUtils.getResultSet(SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), mysqlSql1);

        String mysqlSql3 = String.format("select a.INDEX_NAME, a.NON_UNIQUE, group_concat(COLUMN_NAME) column_names from information_schema.statistics a where TABLE_SCHEMA = '%s' and TABLE_NAME = '%s' GROUP BY a.TABLE_SCHEMA,a.TABLE_NAME,a.index_name;", map.get("-n"), "%s");
        Connection con = SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        createDoc(rs, mysqlSql2, mysqlSql3, map, outFile, true, "MySQL数据库表结构", con);

    }


    private static void createDoc(ResultSet rs, String sqls, String sqls3, Map<String, String> map, String outFile, boolean type, String title, Connection con) throws IOException {
        System.out.println("开始生成文件");
        List<Map<String, String>> list = getTableName(rs);
        RowRenderData header = getHeader();
        RowRenderData header3 = getIndexHeader();
        Map<String, Object> datas = new HashMap<>();
        datas.put("title", title);
        List<Map<String, Object>> tableList = new ArrayList<Map<String, Object>>();
        int i = 0;
        for (Map<String, String> str : list) {
            System.out.println(str);
            if (str.get("table_name").equals("SequelizeMeta") || str.get("table_name").startsWith("migrations")) {
                continue;
            }
            i++;
            String sql = sqls + str.get("table_name") + "'";
            ResultSet set = SqlUtils.getResultSet(con, sql);
            List<RowRenderData> rowList = getRowRenderData(set);
            Map<String, Object> data = new HashMap<>();
            data.put("no", "" + i);
            data.put("table_comment", str.get("table_comment") + "");
            data.put("engine", str.get("engine") + "");
            data.put("table_collation", str.get("table_collation") + "");
            data.put("table_type", str.get("table_type") + "");
            data.put("name", str.get("table_name"));
            data.put("mapName", StringUtils.isNotEmpty(str.get("table_comment")) ? str.get("table_comment") : str.get("table_name"));
            MiniTableRenderData table = new MiniTableRenderData(header, rowList);
            table.setWidth(26.162f);
            data.put("table", table);
            tableList.add(data);

            String sql3 = String.format(sqls3, str.get("table_name"));
            ResultSet set3 = SqlUtils.getResultSet(con, sql3);
            List<RowRenderData> rowList3 = getIndexRenderData(set3);
            MiniTableRenderData table3 = new MiniTableRenderData(header3, rowList3);
            table3.setWidth(26.162f);
            data.put("index", table3);
        }

        datas.put("tablelist", new DocxRenderData(FileUtils.Base64ToFile(outFile, type), tableList));
        XWPFTemplate template = XWPFTemplate.compile(FileUtils.Base64ToInputStream()).render(datas);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            System.out.println("生成文件结束");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("生成文件失败");
        } finally {
            try {
                template.write(out);
                out.flush();
                out.close();
                template.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查缺少的参数
     */
    private static Map<String, String> Check(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String str : args) {
            String[] split = str.split("=");
            map.put(split[0], split[1]);
        }

        if (!map.containsKey("-n")) {
            System.out.println("请输入数据库名称！");
            System.exit(0);
        }
        if (!map.containsKey("-u")) {
            System.out.println("请输入数据库用户名！");
            System.exit(0);
        }
        if (!map.containsKey("-p")) {
            System.out.println("请输入数据库密码！");
            System.exit(0);
        }
        if (!map.containsKey("-d")) {
            System.out.println("请输入保存文件的目录！");
            System.exit(0);
        }
        if (!map.containsKey("h")) {
            System.out.println("请输入地址！");
            System.exit(0);
        }
        if (!map.containsKey("p")) {
            System.out.println("请输入端口");
            System.exit(0);
        }
        return map;
    }

    /**
     * table的表头
     *
     * @return RowRenderData
     */
    private static RowRenderData getHeader() {
        RowRenderData header = RowRenderData.build(
                new TextRenderData("属性中文名称", POITLStyle.getHeaderStyle()),
                new TextRenderData("字段英文名称", POITLStyle.getHeaderStyle()),
                new TextRenderData("业务含义说明", POITLStyle.getHeaderStyle()),
                new TextRenderData("Domain或数据类型", POITLStyle.getHeaderStyle()),
                new TextRenderData("数据长度", POITLStyle.getHeaderStyle()),
                new TextRenderData("小数位数", POITLStyle.getHeaderStyle()),
                new TextRenderData("是否为空", POITLStyle.getHeaderStyle()),
                new TextRenderData("是否为主键", POITLStyle.getHeaderStyle()),
                new TextRenderData("是否为外键", POITLStyle.getHeaderStyle()),
                new TextRenderData("是否是代码", POITLStyle.getHeaderStyle()),
                new TextRenderData("数据标准编号", POITLStyle.getHeaderStyle()),
                new TextRenderData("概念模型编号", POITLStyle.getHeaderStyle())
        );
        header.setRowStyle(POITLStyle.getHeaderTableStyle());
        return header;
    }

    private static RowRenderData getIndexHeader() {
        RowRenderData header = RowRenderData.build(
                new TextRenderData("索引名称", POITLStyle.getHeaderStyle()),
                new TextRenderData("索引字段", POITLStyle.getHeaderStyle()),
                new TextRenderData("是否唯一", POITLStyle.getHeaderStyle())
        );
        header.setRowStyle(POITLStyle.getHeaderTableStyle());
        return header;
    }

    /**
     * 获取索引结构数据
     */
    private static List<RowRenderData> getIndexRenderData(ResultSet set) {
        List<RowRenderData> result = new ArrayList<>();
        try {
            while (set.next()) {
                String uniqStr = set.getString("NON_UNIQUE").equals("0") ? "Y" : "N";
                RowRenderData row = RowRenderData.build(
                        new TextRenderData(set.getString("INDEX_NAME")),
                        new TextRenderData(set.getString("column_names")),
                        new TextRenderData(uniqStr)

                );
                row.setRowStyle(POITLStyle.getBodyTableStyle());
                result.add(row);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取一张表的结构数据
     *
     * @return List<RowRenderData>
     */
    private static List<RowRenderData> getRowRenderData(ResultSet set) {
        List<RowRenderData> result = new ArrayList<>();

        try {
            while (set.next()) {
                String lengthString = set.getString("character_maximum_length") == null ? null : String.format("%s", set.getString("character_maximum_length"));
                String decimalLength = "";
                String isNullStr = set.getString("is_nullable").equals("YES") ? "Y" : "N";
                String isPriStr = set.getString("column_key").contains("PRI") ? "Y" : "N";
                String isOutStr = "N";
                String isCode = "";
                String isStand = "编码型";
                String isModelCode = "";


                String columnType = set.getString("column_type");
                String dataType = set.getString("data_type");
                if (lengthString == null) {
                    if (columnType.contains("(")) {
                        if (dataType.equals("decimal") || dataType.equals("float")) {
                            lengthString = columnType.substring(columnType.indexOf("(") + 1, columnType.indexOf(","));
                            decimalLength = columnType.substring(columnType.indexOf(",") + 1, columnType.indexOf(")"));
                        } else {
                            lengthString = columnType.substring(columnType.indexOf("(") + 1, columnType.indexOf(")"));
                            decimalLength = "";
                        }
                    } else {
                        lengthString = "";
                    }
                }

                String columnComment = set.getString("column_comment");
                String columnName = set.getString("column_name");
                RowRenderData row = RowRenderData.build(
//						new TextRenderData(set.getString("ordinal_position")+""),
                        new TextRenderData(formatComment(columnName, columnComment)),
                        new TextRenderData(columnName),
                        new TextRenderData(formatComment(columnName, columnComment)),
                        new TextRenderData(set.getString("data_type")),
                        new TextRenderData(lengthString),
                        new TextRenderData(decimalLength),
                        new TextRenderData(isNullStr),
                        new TextRenderData(isPriStr),
                        new TextRenderData(isOutStr),
                        new TextRenderData(isCode),
                        new TextRenderData(isStand),
                        new TextRenderData(isModelCode)
//						new TextRenderData(set.getString("character_maximum_length")+""),
//						new TextRenderData(set.getString("is_nullable")+""),
//						new TextRenderData(set.getString("column_default")+""
                );
                row.setRowStyle(POITLStyle.getBodyTableStyle());
                result.add(row);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String formatComment(String columnName, String columnComment) {
        if (StringUtils.isNotEmpty(columnComment) && columnComment.contains("{") && columnComment.contains("}")
                && (columnName.contains("type") || columnName.contains("status"))) {
            System.out.println(columnComment);
            String jsonStr = columnComment.substring(columnComment.indexOf("{"), columnComment.indexOf("}") + 1);
            Map jsonMap = JSON.parseObject(jsonStr, Map.class);
            StringBuilder sb = new StringBuilder(columnComment.substring(0, columnComment.indexOf("{")));
            sb.append(" 枚举值：");
            jsonMap.forEach((key, value) -> {
                sb.append(key).append("(").append(value).append("),");
            });
            return sb.substring(0, sb.length() -1);
        }

        if (columnName.equals("created_at")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "创建时间";
        }
        if (columnName.equals("updated_at")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "更新时间";
        }
        if (columnName.equals("deleted_at")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "删除时间";
        }
        if (columnName.equals("id")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "id";
        }
        if (columnName.equals("user_id")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "用户id";
        }
        if (columnName.equals("job_id")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "任务id";
        }
        if (columnName.equals("group_id")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "组id";
        }
        if (columnName.equals("asset_id")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "资产id";
        }
        if (columnName.equals("status")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "状态";
        }
        if (columnName.equals("type")) {
            return StringUtils.isNotEmpty(columnComment) ? columnComment : "类型";
        }
        if (columnName.equals("compute_type")) {
            return formatComment(columnName, "计算类型{\"PSI\":\"PSI\",\"TRAIN\": \"TRAIN\",\"PREDICT\": \"PREDICT\",\"LPSI\": \"LPSI\"}");
        }
        return StringUtils.isNotEmpty(columnComment) ? columnComment : columnName;
    }

    /**
     * 获取数据库的所有表名及表的信息
     *
     * @return list
     */
    private static List<Map<String, String>> getTableName(ResultSet rs) {
        List<Map<String, String>> list = new ArrayList<>();

        try {
            while (rs.next()) {
                Map<String, String> result = new HashMap<>();
                result.put("table_name", rs.getString("table_name") + "");
                result.put("table_type", rs.getString("table_type") + "");
                result.put("engine", rs.getString("engine") + "");
                result.put("table_collation", rs.getString("table_collation") + "");
                result.put("table_comment", rs.getString("table_comment") + "");
                result.put("create_options", rs.getString("create_options") + "");
                list.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

}
