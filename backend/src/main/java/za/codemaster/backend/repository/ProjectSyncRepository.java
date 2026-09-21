package za.codemaster.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import java.sql.Array;
import java.util.*;

@Repository
public class ProjectSyncRepository {
    public record SyncProject(long id,String owner,String repo,String metadataEtag,String issuesEtag){}
    private final JdbcTemplate jdbc;
    public ProjectSyncRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Optional<SyncProject> findById(long id){
        return jdbc.query("select id, github_owner, github_repo, github_metadata_etag, github_issues_etag from projects where id=?",
            (rs,n)->new SyncProject(rs.getLong("id"),rs.getString("github_owner"),rs.getString("github_repo"),rs.getString("github_metadata_etag"),rs.getString("github_issues_etag")),id).stream().findFirst();
    }
    public boolean isMaintainer(long projectId,long userId){
        Integer n=jdbc.queryForObject("select count(*) from project_maintainers where project_id=? and user_id=?",Integer.class,projectId,userId); return n!=null&&n>0;
    }
    public void updateMetadata(long id, GitHubProjectMetadata m,String etag){
        String[] langs=m.languageBreakdown()==null?new String[0]:m.languageBreakdown().keySet().toArray(String[]::new);
        jdbc.update(c->{var ps=c.prepareStatement("update projects set name=?,description=?,primary_language=?,languages=?,license=?,stars=?,forks=?,open_issues=?,last_activity_at=?,github_metadata_etag=?,updated_at=now() where id=?");
            ps.setString(1,m.name()); ps.setString(2,m.description()); ps.setString(3,m.primaryLanguage()); Array a=c.createArrayOf("text",langs); ps.setArray(4,a); ps.setString(5,m.license()); ps.setInt(6,m.stars()); ps.setInt(7,m.forks()); ps.setInt(8,m.openIssueCount()); ps.setObject(9,m.lastActivityAt()); ps.setString(10,etag); ps.setLong(11,id); return ps;});
    }
    public void updateCommunityProfile(long id,GitHubCommunityProfile profile){
        jdbc.update("update projects set has_contributing_guide=?,has_code_of_conduct=?,updated_at=now() where id=?",
                profile.hasContributingGuide(),profile.hasCodeOfConduct(),id);
    }
    public void updateIssuesEtag(long id,String etag){jdbc.update("update projects set github_issues_etag=?,updated_at=now() where id=?",etag,id);}
}
