package org.example.opensource_rest_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "issue")
public class Label extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long labelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false, foreignKey = @ForeignKey(name = "fk_label_issue"))
    private Issue issue;

    @Column(name = "label_name", nullable = false, length = 100)
    private String labelName;

    @Column(name = "label_color", length = 7)  // #FFFFFF 형식
    private String labelColor;

    // equals와 hashCode (라벨 중복 방지용)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        Label label = (Label) o;
        return labelName != null && labelName.equals(label.labelName) &&
                issue != null && issue.equals(label.issue);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}