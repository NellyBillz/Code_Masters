package za.codemaster.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import java.sql.Array;
import java.util.List;
import java.util.Optional;

@Repository
public class IssueSyncRepository {
    public record ExistingIssue(long id, int githubIssueNumber, Long overriddenBy){}
    private final JdbcTemplate jdbc;
    public IssueSyncRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Optional<ExistingIssue> findByProjectIdAndGithubIssueNumber(long projectId,int number){
        return jdbc.query("select id,github_issue_number,difficulty_overridden_by_user_id from issues where project_id=? and github_issue_number=?",
            (rs,n)->new ExistingIssue(rs.getLong("id"),rs.getInt("github_issue_number"),(Long)rs.getObject("difficulty_overridden_by_user_id")),projectId,number).stream().findFirst();
    }
    public List<ExistingIssue> findOpenByProjectId(long projectId){
        return jdbc.query("select id,github_issue_number,difficulty_overridden_by_user_id from issues where project_id=? and status='open'",
            (rs,n)->new ExistingIssue(rs.getLong("id"),rs.getInt("github_issue_number"),(Long)rs.getObject("difficulty_overridden_by_user_id")),projectId);
    }
    public void insert(long projectId,GitHubIssueMetadata i){
        jdbc.update(c->{var ps=c.prepareStatement("insert into issues(project_id,github_issue_number,github_url,title,body_excerpt,status,labels,difficulty,is_beginner_friendly,created_at,updated_at) values(?,?,?,?,?,'open',?,'unknown',false,?,?)");
            ps.setLong(1,projectId); ps.setInt(2,i.issueNumber()); ps.setString(3,i.htmlUrl()); ps.setString(4,i.title()); ps.setString(5,excerpt(i.body())); Array a=c.createArrayOf("text",i.labels()==null?new String[0]:i.labels().toArray(String[]::new)); ps.setArray(6,a); ps.setObject(7,i.createdAt()); ps.setObject(8,i.updatedAt()); return ps;});
    }
    public void update(long id,GitHubIssueMetadata i){
        jdbc.update(c->{var ps=c.prepareStatement("update issues set github_url=?,title=?,body_excerpt=?,status='open',labels=?,updated_at=? where id=?");
            ps.setString(1,i.htmlUrl()); ps.setString(2,i.title()); ps.setString(3,excerpt(i.body())); Array a=c.createArrayOf("text",i.labels()==null?new String[0]:i.labels().toArray(String[]::new)); ps.setArray(4,a); ps.setObject(5,i.updatedAt()); ps.setLong(6,id); return ps;});
    }
    public boolean markClosed(long id){
        return jdbc.update("update issues set status='closed',updated_at=now() where id=? and status='open'",id)==1;
    }
    private static String excerpt(String body){if(body==null)return null; return body.length()<=1000?body:body.substring(0,1000);}
}
